#!/bin/bash

env_file_name=".env"
telegram_bot_env_file="kolibri-telegram-bot/${env_file_name}"
commandline_utility_env_file="kolibri-commandline-utility/${env_file_name}"

recreate_file() {
  filename=$1
  if [[ -f "$filename" ]]; then
    echo "File $filename exists, delete it? (yes/no)"
    read answer
    if [[ ${answer} == "yes" ]]; then
      rm ${filename}
    else
      echo "Stopped initializing environment"
      exit
    fi
  fi
  touch ${filename}
}

init_telegram_bot_env() {
  recreate_file ${telegram_bot_env_file}
  echo "LOG_FOLDER=/var/log" >> ${telegram_bot_env_file}
  echo "MODULE_NAME=kolibri-telegram-bot" >> ${telegram_bot_env_file}
  echo >> ${telegram_bot_env_file}
  echo "PORT=8080" >> ${telegram_bot_env_file}
  echo "TELEGRAM_BOT_API_URL=https://api.telegram.org" >> ${telegram_bot_env_file}
  echo "TELEGRAM_BOT_TOKEN=" >> ${telegram_bot_env_file}
  echo "TELEGRAM_BOT_URL=" >> ${telegram_bot_env_file}
  echo "TELEGRAM_BOT_OWNER_ID=" >> ${telegram_bot_env_file}
  echo "Initialized telegram bot environment: ${telegram_bot_env_file}"
}

init_commandline_utility_env() {
  recreate_file ${commandline_utility_env_file}
  echo "LOG_FOLDER=/var/log" >> ${commandline_utility_env_file}
  echo "MODULE_NAME=kolibri-commandline-utility" >> ${commandline_utility_env_file}
  echo >> ${commandline_utility_env_file}
  echo "ACCOUNTS_SPREADSHEET_ID=" >> ${commandline_utility_env_file}
  echo "Initialized command line utility environment: ${commandline_utility_env_file}"
}

init_telegram_bot_env
init_commandline_utility_env
