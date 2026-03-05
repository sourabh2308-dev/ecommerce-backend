#!/bin/bash

# Add one-line comments to all Java files based on their purpose
find /workspaces/ecommerce-backend -name "*.java" -path "*/src/main/java/*" | while read file; do
    # Skip if already has multiple comments
    comment_count=$(grep -c "^\s*//" "$file" || echo 0)
    if [ "$comment_count" -gt 20 ]; then
        continue
    fi
    
    basename=$(basename "$file")
    
    # Determine what type of file and add appropriate comment after imports
    if [[ "$basename" == *"Controller.java" ]]; then
        # Add before @RestController
        sed -i '/^@RestController/i // REST API Controller - Handles HTTP requests and responses' "$file"
    elif [[ "$basename" == *"ServiceImpl.java" ]]; then
        # Add before @Service
        sed -i '/^@Service/i // Service Implementation - Contains business logic and data operations' "$file"
    elif [[ "$basename" == *"Repository.java" ]]; then
        # Add before public interface
        sed -i '/^public.*interface.*Repository/i // Data Repository - Provides database access via Spring Data JPA' "$file"
    elif [[ "$basename" == *"Entity.java" ]]; then
        # Add before @Entity
        sed -i '/^@Entity/i // JPA Entity - Domain model persisted in database' "$file"
    elif [[ "$basename" == *"Request.java" ]]; then
        # Add before @Data
        sed -i '/^@Data/i // DTO Request - Data transfer object for incoming API requests' "$file"
    elif [[ "$basename" == *"Response.java" ]]; then
        # Add before @Data
        sed -i '/^@Data/i // DTO Response - Data transfer object for outgoing API responses' "$file"
    elif [[ "$basename" == *"Exception.java" ]]; then
        # Add before class
        sed -i '/^public.*class.*Exception/i // Custom Exception - Domain-specific error handling' "$file"
    elif [[ "$basename" == *"Config.java" ]] || [[ "$basename" == *"Configuration.java" ]]; then
        # Add before @Configuration
        sed -i '/^@Configuration/i // Spring Configuration - Defines beans and infrastructure setup' "$file"
    elif [[ "$basename" == *"Filter.java" ]]; then
        # Add before class
        sed -i '/^public.*class.*Filter/i // HTTP Filter - Intercepts requests for cross-cutting concerns' "$file"
    fi
done

echo "Comments added to all Java files successfully!"
