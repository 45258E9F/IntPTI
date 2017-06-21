import cgi
import json
import os
import thread
import urlparse
import webbrowser
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler

PORT_NUMBER = 9026
current_dir = os.path.dirname(os.path.realpath(__file__))
meta_file = ["from_java", "fileInfo.json"]
file_dict = {}
meta_fix = ["from_java", "fixInfo.json"]
fix_data = None
# the currently chosen source file
current_file = None
# flatted fix data is updated when a new file is chosen
flat_fixdata = {}
# the cache for selected fixes (applicable only when the front-end works in the manual mode)
selected_fix = {}
committed_fix = ["from_server", "fix.json"]


def parse_fixdata(filename, selected_mode):
    global fix_data
    if fix_data is None:
        meta_fix_path = os.path.join(current_dir, meta_fix[0], meta_fix[1])
        try:
            fix_data = json.load(open(meta_fix_path))
        except:
            fix_data = {}
    r = []
    fix_list = fix_data.get(filename)
    if fix_list is not None:
        r.extend(parse_fixlist(fix_list, 0, selected_mode))
    return r


def parse_fixlist(sub_list, indent, selected_mode):
    global flat_fixdata
    r = []
    indent_str = "%d" % (indent * 5) + "%"
    for i in range(0, len(sub_list)):
        fix_entry = sub_list[i]
        uuid = fix_entry["UUID"]
        start_line = fix_entry["startLine"]
        end_line = fix_entry["endLine"]
        fix_mode = fix_entry["mode"]
        item_class = "item intfix"
        r.append('<div class="' + item_class + '" id="' + uuid + '" data-indent="' + str(indent) +
                 '" style="margin-left: ' + indent_str + '">')
        r.append('<div class="left floated content">')
        # if the manual mode is chosen, the generated items should be inactive by default
        if selected_mode == 'Manual':
            r.append('<button class="circular ui icon toggle button">')
        else:
            r.append('<button class="circular ui icon toggle button active">')
        r.append('<i class="bug icon"></i>')
        r.append('</button>')
        r.append('</div>')
        r.append('<div class="content">')
        r.append('<div class="header">')
        if start_line == end_line:
            r.append("Line " + str(start_line))
        else:
            r.append("Line " + str(start_line) + "-" + str(end_line))
        r.append('</div>')
        r.append('<div class="description">')
        if fix_mode == "CAST":
            r.append('explicit cast')
        elif fix_mode == "SPECIFIER":
            r.append('declared type change')
        else:
            r.append('sanity check')
        r.append('</div></div></div>')
        # store the fix record for marker purpose
        flat_fixdata[uuid] = {key: fix_entry[key] for key in fix_entry if key != "children"}
        # handle possible sub-fixes which rely on the current fix
        subfix_list = fix_entry["children"]
        if len(subfix_list) > 0:
            r.extend(parse_fixlist(subfix_list, indent + 1, selected_mode))
    return r


def init_filedict():
    flat_dict = {}
    meta_file_path = os.path.join(current_dir, meta_file[0], meta_file[1])
    try:
        file_data = json.load(open(meta_file_path))
        init_filedict_json(file_data, [], flat_dict)
    except:
        flat_dict = {}
    return flat_dict


def init_filedict_json(file_data, prefix, flat_dict):
    parent_path = ''
    if len(prefix) != 0:
        parent_path = os.path.join(*prefix)
    for entry in file_data:
        name = entry.get("name")
        if parent_path in flat_dict:
            flat_dict[parent_path].append(name)
        else:
            flat_dict[parent_path] = [name]
        sub_items = entry.get("children")
        prefix.append(name)
        init_filedict_json(sub_items, prefix, flat_dict)
        prefix.pop()


def parse_filetree(root_dir):
    global file_dict
    r = ['<ul class="jqueryFileTree" style="display: none;">']
    file_list = file_dict.get(root_dir)
    if file_list is None:
        r.append('</ul>')
        return r
    folders = {}
    files = {}
    for name in file_list:
        path = os.path.join(root_dir, name)
        if os.path.isfile(path):
            files[name] = path
        elif os.path.isdir(path):
            folders[name] = path
    for name in sorted(folders):
        path = folders[name]
        r.append('<li class="directory collapsed"><a rel="%s">%s</a></li>' % (path, name))
    for name in sorted(files):
        path = files[name]
        r.append('<li class="file ext_c"><a rel="%s">%s</a></li>' % (path, name))
    r.append('</ul>')
    return r


class SimpleHandler(BaseHTTPRequestHandler):

    to_exit = False

    def do_GET(self):
        r = []
        url_parsed = urlparse.urlparse(self.path)
        param_dict = urlparse.parse_qs(url_parsed.query)
        f = url_parsed.path
        if f != '/':
            # applicable for HTML and relevant resources
            f = f[1:]
            fp = open(os.path.join(current_dir, f))
            self.send_response(200)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(fp.read())
            fp.close()
            return
        if 'file' in param_dict:
            try:
                r = []
                f = ''.join(param_dict['file'])
                f = urlparse.unquote(f)
                fp = open(f)
                source = fp.read()
                fp.close()
                cgi.escape(source)
                r.append(source)
            except:
                r.append("Could not load the file: %s " % f)
        try:
            self.send_response(200)
            self.send_header("Welcome", "Contact")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(''.join(r).encode())
        except Exception as e:
            print("Error in sending response: %s" % str(e))

    def do_POST(self):
        global flat_fixdata
        global current_file
        global selected_fix
        r = []
        ctype, pdict = cgi.parse_header(self.headers.getheader('content-type'))
        if ctype == 'multipart/form-data':
            postvars = cgi.parse_multipart(self.rfile, pdict)
        elif ctype == 'application/x-www-form-urlencoded':
            length = int(self.headers.getheader('content-length'))
            postvars = urlparse.parse_qs(self.rfile.read(length), keep_blank_values=1)
        else:
            postvars = {}
        if 'dir' in postvars:
            root_dir = ''.join(postvars['dir'])
            r = parse_filetree(root_dir)
        elif 'req' in postvars:
            req_id = ''.join(postvars['req'])
            if req_id == 'fixList':
                filename = ''.join(postvars['file'])
                selected_mode = ''.join(postvars['mode'])
                current_file = filename
                flat_fixdata = {}
                r = parse_fixdata(filename, selected_mode)
            elif req_id == 'fixDraw':
                fix_id = ''.join(postvars['id'])
                requested_dict = flat_fixdata.get(fix_id)
                if requested_dict is not None:
                    r.append(json.dumps(requested_dict))
        elif 'op' in postvars:
            op_id = ''.join(postvars['op'])
            if op_id == 'clear':
                selected_fix = {}
            elif op_id == 'cache':
                filename = ''.join(postvars['file'])
                selected = ''.join(postvars['list'])
                fixes = selected.split(",")
                selected_fix[filename] = fixes
            elif op_id == 'close':
                selected_mode = ''.join(postvars['mode'])
                if selected_mode != 'Manual':
                    selected_fix = {}
                # append the mode identifier into the JSON
                selected_fix['_mode_'] = selected_mode
                fix_file = os.path.join(current_dir, committed_fix[0], committed_fix[1])
                # write the cached results into the file
                with open(fix_file, 'w') as fp:
                    json.dump(selected_fix, fp)
                print("WARNING: server is terminated")
                self.to_exit = True
        try:
            self.send_response(200)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(''.join(r).encode())
        except Exception as e:
            print("Error in sending response: %s" % str(e))
        if self.to_exit:
            def kill_server(server):
                server.shutdown()
            thread.start_new_thread(kill_server, (httpd,))


if __name__ == '__main__':
    file_dict = init_filedict()
    httpd = HTTPServer(('', PORT_NUMBER), SimpleHandler)
    webbrowser.open("http://localhost:" + str(PORT_NUMBER) + "/index.html")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
