import os
import sys
import json
import subprocess
import socket
import http.server
import socketserver

def get_local_ip():
    """Get the local IP address of the machine on the network."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def chunk_book(file_path):
    """Parse sayGoodbyeToInsomenia.txt and split it into logical chunks."""
    if not os.path.exists(file_path):
        print(f"Error: Book not found at {file_path}")
        return []
        
    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
        text = f.read()
        
    paragraphs = text.split("\n\n")
    chunks = []
    current_chunk = []
    current_len = 0
    
    for p in paragraphs:
        p_clean = p.strip()
        p_clean = " ".join(p_clean.split())
        if not p_clean:
            continue
            
        current_chunk.append(p_clean)
        current_len += len(p_clean)
        
        # When chunk size is roughly 1000 characters, we finalize it
        if current_len >= 1000:
            chunks.append(" ".join(current_chunk))
            # Keep 1 paragraph overlap for continuity
            current_chunk = [current_chunk[-1]] if len(current_chunk) > 1 else []
            current_len = sum(len(c) for c in current_chunk)
            
    if current_chunk:
        chunks.append(" ".join(current_chunk))
        
    return chunks

def run_command(command, cwd=None):
    """Helper to run a shell command and print output."""
    print(f"Executing: {' '.join(command)} in {cwd or '.'}")
    result = subprocess.run(command, cwd=cwd, shell=False)
    if result.returncode != 0:
        print(f"Command failed with exit code {result.returncode}")
        sys.exit(result.returncode)

def main():
    root_dir = os.path.dirname(os.path.abspath(__file__))
    book_path = os.path.join(root_dir, "sayGoodbyeToInsomenia.txt")
    assets_dir = os.path.join(root_dir, "frontend", "src", "assets")
    chunks_json_path = os.path.join(assets_dir, "book_chunks.json")
    
    # 1. Ensure assets directory exists
    os.makedirs(assets_dir, exist_ok=True)
    
    # 2. Parse and generate book chunks JSON
    print("Parsing book sayGoodbyeToInsomenia.txt...")
    chunks = chunk_book(book_path)
    if not chunks:
        print("Warning: Could not chunk book. An empty list will be used.")
    with open(chunks_json_path, "w", encoding="utf-8") as f:
        json.dump(chunks, f, indent=2)
    print(f"Successfully generated {len(chunks)} book chunks at {chunks_json_path}")
    
    # 2.5 Extract API Key from .env and write to env.js
    env_path = os.path.join(root_dir, ".env")
    api_key = ""
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.startswith("DEEPSEEK_API_KEY="):
                    api_key = line.split("=", 1)[1].strip()
                    break
    
    public_dir = os.path.join(root_dir, "frontend", "public")
    os.makedirs(public_dir, exist_ok=True)
    env_js_path = os.path.join(public_dir, "env.js")
    with open(env_js_path, "w", encoding="utf-8") as f:
        f.write(f'window.DEEPSEEK_API_KEY = "{api_key}";\n')
    print(f"Pre-configured DeepSeek API key in public/env.js (extracted from local .env)")
    
    # 3. Setup and Build Frontend
    frontend_dir = os.path.join(root_dir, "frontend")
    node_modules = os.path.join(frontend_dir, "node_modules")
    
    if not os.path.exists(node_modules):
        print("Installing frontend dependencies (npm install)...")
        run_command(["npm", "install"], cwd=frontend_dir)
        
    print("Building frontend PWA (npm run build)...")
    run_command(["npm", "run", "build"], cwd=frontend_dir)
    
    # 5. Serve static files using Python's built-in http.server
    dist_dir = os.path.join(frontend_dir, "dist")
    os.chdir(dist_dir)
    
    class CustomHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
        # Prevent caching for development/updates
        def end_headers(self):
            self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
            super().end_headers()
            
    # Allow port reuse to avoid 'port in use' errors on quick restarts
    socketserver.TCPServer.allow_reuse_address = True
    
    port = 8000
    httpd = None
    while port < 8020:
        try:
            httpd = socketserver.TCPServer(("", port), CustomHTTPRequestHandler)
            break
        except OSError:
            port += 1
            
    if not httpd:
        print("Error: Could not find an available port to start the server.")
        sys.exit(1)
        
    # 4. Print instructions
    local_ip = get_local_ip()
    print("\n" + "="*60)
    print(" AI Sleep Advisor PWA is built and ready to install!")
    print(f" Local Desktop Access: http://localhost:{port}")
    print(f" Mobile network access: http://{local_ip}:{port}")
    print(f" Open http://{local_ip}:{port} on your Pixel 10 phone (same Wi-Fi)")
    print(" and click 'Add to Home Screen' to run as a voice-enabled app!")
    print("="*60 + "\n")
    
    print(f"Serving sleep advisor at port {port}...")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server.")
        sys.exit(0)

if __name__ == "__main__":
    main()
