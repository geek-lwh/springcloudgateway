-- 压测lua脚本

wrk.method = "GET"
wrk.headers["Content-Type"] = "application/json"
wrk.headers["Authorization"] = "Basic c2Vydi1hdXRoOmV5SjBlWEFpT2lKcWQzUWlMQ0poYkdjaU9pSklVekkxTmlKOS5leUp2Y0dWdVNXUWlPbTUxYkd3c0luTmxjM05wYjI1T1lXMWxJam9pYlc5NmFXeHNZUzgxTGpBZ0tHMWhZMmx1ZEc5emFEc2dhVzUwWld3Z2JXRmpJRzl6SUhnZ01UQmZNVFJmTkNrZ1lYQndiR1YzWldKcmFYUXZOVE0zTGpNMklDaHJhSFJ0YkN3Z2JHbHJaU0JuWldOcmJ5a2dZMmh5YjIxbEx6Y3pMakF1TXpZNE15NDROaUJ6WVdaaGNta3ZOVE0zTGpNMklpd2liVzlpYVd4bElqb2lNVFUyTVRnd05EQXdPRFFpTENKcGMzTWlPaUpoYUdFdGNHRnpjM0J2Y25RdGMyVnlkbVZ5SWl3aVlYWmhkR0Z5SWpvaUlpd2ljMlZ6YzJsdmJrbGtJam9pTnpkbVlqUTNaalprWlRjeFpUTXdZMkkyT0RsaFl6YzVNRGcxTlRobE1qZ2lMQ0oxYzJWeVNXUWlPakl6TVRZeUxDSjJaWEp6YVc5dUlqb2lkaklpTENKamJHbGxiblJVZVhCbElqb3lMQ0p1WVcxbElqb2lJaXdpWlhod0lqb3hOVFUwT0RBNU5EWTRMQ0pwWVhRaU9qRTFOVFEzTnpNME5qZ3NJbXAwYVNJNklqYzNabUkwTjJZMlpHVTNNV1V6TUdOaU5qZzVZV00zT1RBNE5UVTRaVEk0WHpFeU1qZzRNVEExT0RVaWZRLlpvT0JvZVB6UkVEb2dWTWlKYm4wbnZJZXBzaUFhOHRHRGhnYXhiMERvajg="
wrk.headers["User-Agent"] = "ahaschool/ios/4.3.0/12.0/iPhone/757797D4-1282-4668-8AA3-FE4189AAED12"
wrk.headers["X-Env"] = "eyJ1dG1fc291cmNlIjoiIiwidXRtX21lZGl1bSI6IiIsInV0bV9jYW1wYWlnbiI6IiIsInV0bV90ZXJtIjoiIiwidXRtX2NvbnRlbnQiOiIiLCJwayI6Ikx6WTVOVE09IiwicGQiOiIiLCJwcyI6InVwNDY2MDhiMGZjMTA1MDBlNzgyYTVjYmJiZTg2M2E1YWUiLCJwcCI6IiIsImFwcF90eXBlIjoxLCJndW5pcWlkIjoiZWM3NjdhZjI5ZDY5Zjg0YjViYjEwZGFmOGNiZjk1OGIiLCJjaGFubmVsIjoiYmFpZHUifQ=="
wrk.headers["X-Forwarded-For"] = "127.1.1.1,127.1.1.2"


-- 压测命令
-- wrk -c 30 -t 4 -d 30s -s request.lua http://localhost:9700/v3/users/info --latency