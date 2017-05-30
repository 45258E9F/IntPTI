import cgi
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
    for entry in file_data:
        name = entry.get("name")
        if parent_path is not None:
            path = os.path.join(parent_path, name)
        else:
            path = name
        if os.path.isfile(path):
            r.append('<li class="file ext_c"><a rel="%s">%s</a></li>' % (path, name))
        elif os.path.isdir(path):
            r.append('<li class="directory collapsed"><a rel="%s">%s</a>' % (path, name))
            sub_paths = entry.get("children")
            prefix.append(name)
            r.extend(parse_filetree_json(sub_paths, prefix))
            prefix.pop()
            r.append('</li>')
    r.append('</ul>')
    return r


class SimpleHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        r = []
        url_parsed = urlparse.urlparse(self.path)
        param_dict = urlparse.parse_qs(url_parsed.query)
        f = url_parsed.path
        if f != '/':
            f = f[1:]
            fp = open(os.path.join(current_dir, f))
            self.send_response(200)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(fp.read())
            fp.close()
            return
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
        dir_id = postvars.get('dir')
        if dir_id[0] == 'fileTree':
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
