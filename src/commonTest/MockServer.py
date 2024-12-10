import http.server
import socketserver
import threading
import json
import time

class MockHTTPRequestHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        # Example response for GET requests
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "OK", "message": "This is a mock response"}).encode())

    def do_POST(self):
        # Example response for POST requests
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        print(post_data.decode())
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "OK", 
                                     "received": post_data.decode(),
                                     "requestline": self.requestline,
                                     "headers": str(self.headers),
                                     }).encode())

def run_mock_server(port=8000):
    handler = MockHTTPRequestHandler
    with socketserver.TCPServer(("", port), handler) as httpd:
        print(f"Mock server running on port {port}")
        httpd.serve_forever()

# Start the server in a separate thread so that the script doesn't block
server_thread = threading.Thread(target=run_mock_server, args=(8000,))
server_thread.daemon = True  # Set as daemon so it stops when the main thread exits
server_thread.start()

# Here you would run your client code or tests that interact with the mock server
# For example:
# Your client code or tests go here, using "http://localhost:8000" as the endpoint

# Optionally, to stop the server manually, you could add:
# server_thread.join()


time.sleep(1000)
