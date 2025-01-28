#!/bin/bash

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
