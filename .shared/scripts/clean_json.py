import json
import sys

def extract_first_json(file_path):
    try:
        with open(file_path, 'r') as f:
            content = f.read()
        brace_count = 0
        start_idx = None
        for i, char in enumerate(content):
            if char == '{':
                if brace_count == 0:
                    start_idx = i
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0 and start_idx is not None:
                    json_str = content[start_idx:i+1]
                    try:
                        json_obj = json.loads(json_str)
                        with open(file_path + '.tmp', 'w') as out:
                            json.dump(json_obj, out, indent=2)
                        print(f'Successfully cleaned {file_path}')
                        return True
                    except json.JSONDecodeError as e:
                        print(f'Error parsing JSON in {file_path}: {e}')
                        return False
        print(f'No valid JSON found in {file_path}')
        return False
    except Exception as e:
        print(f'Error processing {file_path}: {e}')
        return False

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print('Usage: python clean_json.py <file_path>')
        sys.exit(1)
    file_path = sys.argv[1]
    success = extract_first_json(file_path)
    sys.exit(0 if success else 1)