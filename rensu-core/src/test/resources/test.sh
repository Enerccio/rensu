export TESSDATA_PREFIX=/usr/share/tessdata/
tesseract "$1" /tmp/x.txt -l jpn
cat /tmp/x.txt