import cgi
import collections
import os
import urlparse
import webbrowser
import json
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler

PORT_NUMBER = 9026
current_dir = os.path.dirname(os.path.realpath(__file__))
meta_file = ["from_java", "fileInfo.json"]
meta_fix = ["from_java", "fixInfo.json"]
fix_data = None
# the currently chosen source file
current_file = None
# flatted fix data is updated when a new file is chosen
flat_fixdata = {}
# the cache for selected fixes (applicable only when the front-end works in the manual mode)
selected_fix = {}


def parse_fixdata(filename):
    global fix_data
    if fix_data is None:
        meta_fix_path = os.path.join(current_dir, meta_fix[0], meta_fix[1])
        try:
            fix_data = json.load(open(meta_fix_path))
        except Exception:
            fix_data = {}
    r = []
    fix_list = fix_data.get(filename)
    if fix_list is not None:
        r.extend(parse_fixlist(fix_list, 0))
    return r


def parse_fixlist(sub_list, indent):
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
        flat_fixdata[uuid] = {"startLine": int(start_line), "endLine": int(end_line),
                              "startOffset": int(fix_entry["startOffset"]), "endOffset": int(fix_entry["endOffset"])}
        # handle possible sub-fixes which rely on the current fix
        subfix_list = fix_entry["children"]
        if len(subfix_list) > 0:
            r.extend(parse_fixlist(subfix_list, indent + 1))
    return r


def parse_filetree():
    meta_file_path = os.path.join(current_dir, meta_file[0], meta_file[1])
    try:
        file_data = json.load(open(meta_file_path))
        r = parse_filetree_json(file_data, [])
    except Exception:
        r = ['<ul class="jqueryFileTree" style="display: none;">', 'Could not load directory: %s' % str(meta_file_path),
             '</ul>']
    return r


def parse_filetree_json(file_data, prefix):
    r = ['<ul class="jqueryFileTree" style="display: none;">']
    parent_path = None
    if len(prefix) != 0:
        parent_path = os.path.join(*prefix)
    folders = {}
    files = {}
    for entry in file_data:
        name = entry.get("name")
        if parent_path is not None:
            path = os.path.join(parent_path, name)
        else:
            path = name
        if os.path.isfile(path):
            files[name] = entry
        elif os.path.isdir(path):
            folders[name] = entry
    sorted_folders = collections.OrderedDict(sorted(folders.items(), key=lambda t: t[0]))
    sorted_files = collections.OrderedDict(sorted(files.items(), key=lambda t: t[0]))
    for name, entry in sorted_folders.iteritems():
        if parent_path is not None:
            path = os.path.join(parent_path, name)
        else:
            path = name
        r.append('<li class="directory collapsed"><a rel="%s">%s</a>' % (path, name))
        sub_paths = entry.get("children")
        prefix.append(name)
        r.extend(parse_filetree_json(sub_paths, prefix))
        prefix.pop()
        r.append('</li>')
    for name, entry in sorted_files.iteritems():
        if parent_path is not None:
            path = os.path.join(parent_path, name)
        else:
            path = name
        r.append('<li class="file ext_c"><a rel="%s">%s</a></li>' % (path, name))
    r.append('</ul>')
    return r


class SimpleHandler(BaseHTTPRequestHandler):
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
            except Exception:
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
            dir_id = ''.join(postvars['dir'])
            if dir_id == 'fileTree':
                r = parse_filetree()
            elif dir_id == 'fixList':
                filename = ''.join(postvars['file'])
                current_file = filename
                flat_fixdata = {}
                r = parse_fixdata(filename)
            elif dir_id == 'fixDraw':
                fix_id = ''.join(postvars['id'])
                requested_dict = flat_fixdata.get(fix_id)
                if requested_dict is not None:
                    r.append(json.dumps(requested_dict))
        if 'op' in postvars:
            op_id = ''.join(postvars['op'])
            if op_id == 'clear':
                selected_fix = {}
            elif op_id == 'cache':
                filename = ''.join(postvars['file'])
                selected = ''.join(postvars['list'])
                fixes = selected.split(",")
                selected_fix[filename] = fixes
                print(selected_fix)
        try:
            self.send_response(200)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(''.join(r).encode())
        except Exception as e:
            print("Error in sending response: %s" % str(e))


if __name__ == '__main__':
    httpd = HTTPServer(('', PORT_NUMBER), SimpleHandler)
    webbrowser.open("http://localhost:" + str(PORT_NUMBER) + "/index.html")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
