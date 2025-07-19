#!/bin/bash
echo "Resetting database and EFS..."
ssh -i "D:/Code Files/Projects/FileStorm/ssh/FileStormServer.pem" admin@filestorm.pro << 'EOF'
  sudo -u postgres psql -c "DROP DATABASE IF EXISTS filestorm WITH (FORCE);"
  sudo -u postgres psql -c "CREATE DATABASE filestorm;"
  sudo rm -rf /mnt/efs/fs1/filestorm_storage/*
EOF
echo "Reset done."