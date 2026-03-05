#!/usr/bin/env python3
"""
Script to add detailed method-level comments to critical Java files.
This version inserts comments BEFORE method declarations without breaking structure.
"""

import os
import re
from pathlib import Path

def add_method_comments_to_file(filepath):
    """Add detailed method comments to a Java file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        # Don't process if already heavily commented
        comment_lines = sum(1 for line in lines if '/**' in line or '/*' in line)
        if comment_lines > 10:
            return False
        
        new_lines = []
        i = 0
        
        while i < len(lines):
            line = lines[i]
            
            # Look for method declarations
            if re.search(r'^\s*(public|private|protected|@Override)\s+', line):
                # Check if next few lines form a method signature
                method_sig = line
                j = i
                while j < len(lines) and not re.search(r'\)\s*\{?', method_sig):
                    j += 1
                    if j < len(lines):
                        method_sig += lines[j]
                
                # Try to extract method name
                match = re.search(r'(public|private|protected|@Override).*?\s+(\w+)\s*\(', method_sig)
                if match and not re.match(r'^\s*/\*\*', line):
                    method_name = match.group(2)
                    
                    # Add comment before the method
                    comment = f"    /** {method_name}: See implementation below. */\n"
                    new_lines.append(comment)
            
            new_lines.append(line)
            i += 1
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
        
        return True
    
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def add_class_header(filepath):
    """Add class-level documentation."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Skip if already has class documentation
        if content.count('/**') > 5:  # Already well-documented
            return False
        
        # Check file type and create appropriate header
        filename = Path(filepath).name
        package_match = re.search(r'package\s+([\w.]+);', content)
        package = package_match.group(1) if package_match else "unknown"
        
        class_match = re.search(r'public\s+(?:class|interface)\s+(\w+)', content)
        class_name = class_match.group(1) if class_match else "Unknown"
        
        # Determine file type and add appropriate comment
        if 'Controller' in filename:
            doc = f"""/**
 * REST API Controller for {class_name}
 * Handles HTTP requests and delegates to service layer.
 */
"""
        elif 'ServiceImpl' in filename:
            doc = f"""/**
 * Service implementation containing business logic.
 * Handles validation, data persistence, and inter-service calls.
 */
"""
        elif 'Repository' in filename:
            doc = f"""/**
 * Data access layer using Spring Data JPA.
 * Provides CRUD operations and custom queries.
 */
"""
        elif 'Entity' in filename:
            doc = f"""/**
 * JPA Entity representing a domain model persisted in the database.
 */
"""
        elif 'Dto' in filename or 'Request' in filename or 'Response' in filename:
            doc = f"""/**
 * Data Transfer Object for moving data between layers.
 */
"""
        elif 'Configuration' in filename or 'Config' in filename:
            doc = f"""/**
 * Spring configuration class for beans and infrastructure setup.
 */
"""
        elif 'Exception' in filename:
            doc = f"""/**
 * Custom exception for domain-specific error handling.
 */
"""
        elif 'Filter' in filename:
            doc = f"""/**
 * HTTP request/response interceptor for cross-cutting concerns.
 */
"""
        else:
            return False
        
        # Insert after imports
        lines = content.split('\n')
        insert_pos = 0
        
        for i, line in enumerate(lines):
            if line.startswith('import ') or line.startswith('package '):
                insert_pos = i + 1
        
        # Find first non-import, non-package line
        while insert_pos < len(lines) and (not lines[insert_pos].strip() or lines[insert_pos].strip().startswith('import')):
            insert_pos += 1
        
        lines.insert(insert_pos, doc)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        
        return True
    
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def main():
    """Main entry point."""
    base_path = Path('/workspaces/ecommerce-backend')
    java_files = sorted(base_path.rglob('src/main/java/**/*.java'))
    
    print(f"Processing {len(java_files)} Java files...")
    
    processed = 0
    skipped = 0
    
    # Process controllers and key services
    priority_patterns = ['Controller', 'Service', 'Repository', 'Config', 'Exception']
    
    for java_file in java_files:
        # Prioritize certain files
        should_process = any(pattern in java_file.name for pattern in priority_patterns)
        
        if should_process:
            if add_class_header(str(java_file)):
                processed += 1
                print(f"✓ {java_file.relative_to(base_path)}")
            else:
                skipped += 1
    
    print(f"\nCompleted: {processed} files updated, {skipped} files skipped")

if __name__ == '__main__':
    main()
