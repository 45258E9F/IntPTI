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
            if ''.join(postvars['dir']) == 'fileTree':
                r = parse_filetree()
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
