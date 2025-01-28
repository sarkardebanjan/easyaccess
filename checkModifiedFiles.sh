#!/bin/bash

# Example usage:
# LAST_VALID_MODIFIED_DATE="2024-12-01"
# DIRECTORIES_TO_SCAN=("dir1" "dir2" "dir3")

# Ensure LAST_VALID_MODIFIED_DATE and DIRECTORIES_TO_SCAN are set
if [[ -z "$LAST_VALID_MODIFIED_DATE" || ${#DIRECTORIES_TO_SCAN[@]} -eq 0 ]]; then
  echo "LAST_VALID_MODIFIED_DATE or DIRECTORIES_TO_SCAN is not set."
  exit 1
fi

# Convert LAST_VALID_MODIFIED_DATE to the timestamp format
MODIFIED_DATE_TIMESTAMP=$(date -d "$LAST_VALID_MODIFIED_DATE" +%s)

# Create the output file
OUTPUT_FILE="/tmp/files_modified_after_date.txt"
> "$OUTPUT_FILE"  # Clear the file if it already exists

# Use find to check modification time and write full paths to the output file
for dir in "${DIRECTORIES_TO_SCAN[@]}"; do
  find "$dir" -type f -newermt "$LAST_VALID_MODIFIED_DATE" >> "$OUTPUT_FILE" 2>/dev/null
done

echo "Files modified after $LAST_VALID_MODIFIED_DATE have been listed in $OUTPUT_FILE."

# Ensure required variables are set
if [[ -z "$LAST_VALID_MODIFIED_DATE" || -z "$OUTPUT_FILE" || ${#ZIPPABLE_FILE_TYPES[@]} -eq 0 || ${#NON_ZIPPABLE_FILE_TYPES[@]} -eq 0 ]]; then
  echo "Required variables (LAST_VALID_MODIFIED_DATE, OUTPUT_FILE, ZIPPABLE_FILE_TYPES, NON_ZIPPABLE_FILE_TYPES) are not set."
  exit 1
fi

# Output files
ZIP_FILE="MODIFIED_CONFIG_FILES.zip"
TXT_FILE="MODIFIED_LIB_FILES.txt"

# Clear or create output files
> "$ZIP_FILE"
> "$TXT_FILE"

# Process files from $OUTPUT_FILE
while IFS= read -r file; do
  # Get the file extension
  file_extension="${file##*.}"
  
  # Check if the extension is in ZIPPABLE_FILE_TYPES
  if [[ " ${ZIPPABLE_FILE_TYPES[@]} " =~ " $file_extension " ]]; then
    zip -q -u "$ZIP_FILE" "$file"  # Add the file to the ZIP archive
  elif [[ " ${NON_ZIPPABLE_FILE_TYPES[@]} " =~ " $file_extension " ]]; then
    echo "$file" >> "$TXT_FILE"  # Add the file name to the TXT file
  fi
done < "$OUTPUT_FILE"

# Get the hostname
HOSTNAME=$(hostname)

# Compose the email body with bold and red warning
EMAIL_BODY="<html>
<body>
<p><span style=\"color:red; font-weight:bold;\">Warning!</span></p>
<p>Files modified after $LAST_VALID_MODIFIED_DATE detected on the server: <b>$HOSTNAME</b>.</p>
<p>Please review at the earliest.</p>
</body>
</html>"

# Send the email with attachments
echo "$EMAIL_BODY" | mail -s "Warning! Files modified after $LAST_VALID_MODIFIED_DATE detected" \
  -a "$ZIP_FILE" -a "$TXT_FILE" -a "Content-Type: text/html" xyz@in.com

echo "Email sent to xyz@in.com with attachments $ZIP_FILE and $TXT_FILE."
