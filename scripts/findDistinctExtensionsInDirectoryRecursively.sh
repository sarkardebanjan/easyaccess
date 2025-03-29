find /path/to/directory -type f | sed -n 's/.*\.\([^.]*\)$/\1/p' | sort -u
