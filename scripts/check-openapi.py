#!/usr/bin/env python3
"""Check OpenAPI local references and its parity with controller routes."""
import json
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
spec = json.loads((root / 'docs/openapi.json').read_text())
assert spec['openapi'].startswith('3.1.'), 'Expected OpenAPI 3.1'

def visit(value):
    if isinstance(value, dict):
        if '$ref' in value:
            reference = value['$ref']
            assert reference.startswith('#/'), 'Only local references are expected'
            target = spec
            for key in reference[2:].split('/'):
                target = target[key.replace('~1', '/').replace('~0', '~')]
        for child in value.values():
            visit(child)
    elif isinstance(value, list):
        for child in value:
            visit(child)

visit(spec)
source = (root / 'src/main/java/dev/bagdaulet/gradescale/ApiController.java').read_text()
actual = {(verb.lower(), path) for verb, path in re.findall(r'@(Get|Post|Put|Patch|Delete)Mapping\("([^"]+)"\)', source)}
declared = {(verb, path) for path, operations in spec['paths'].items() for verb in operations if verb in {'get','post','put','patch','delete'}}
assert actual == declared, f'Controller/OpenAPI route mismatch: {actual ^ declared}'
for path, operations in spec['paths'].items():
    for operation in operations.values():
        assert operation['responses'], f'Missing responses at {path}'
        if '{id}' in path:
            assert any(p['name']=='id' and p['in']=='path' and p['required'] for p in operation['parameters'])
print('OpenAPI references and all', len(actual), 'controller operations match.')
