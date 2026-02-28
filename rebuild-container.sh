#!/bin/bash
# Script to rebuild the devcontainer

echo "Attempting to rebuild DevContainer..."
echo "Note: You may need to use the VS Code UI instead (Ctrl+Shift+P > Rebuild Container)"

# Try using devcontainer CLI if available
if command -v devcontainer &> /dev/null; then
    echo "Found devcontainer CLI, attempting rebuild..."
    devcontainer rebuild --workspace-folder /workspaces/ecommerce-backend
else
    echo "devcontainer CLI not found in PATH"
    echo ""
    echo "FALLBACK: Please use VS Code UI:"
    echo "1. Press Ctrl+Shift+P (or Cmd+Shift+P on Mac)"
    echo "2. Type: Dev Containers: Rebuild Container"
    echo "3. Press Enter"
    echo "4. Wait 3-5 minutes for rebuild to complete"
    echo ""
    echo "OR run from local machine:"
    echo "  gh codespace rebuild -c <codespace-name>"
fi
