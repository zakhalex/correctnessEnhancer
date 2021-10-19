#!/bin/bash
echo "qsub ${1}"

qsub <<EOF
${1}
EOF
