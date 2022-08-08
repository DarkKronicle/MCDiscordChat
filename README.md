<div align="right">
Language: English <a href="/README_CN.md">中文</a>
</div>

<p align="center">
<img width=128 src="https://cdn.jsdelivr.net/gh/Xujiayao/MCDiscordChat@master/src/main/resources/assets/mcdiscordchat/icon.png">
</p>

# MCDiscordChat

MCDiscordChat (MCDC), a practical and powerful Fabric and Quilt Minecraft <> Discord chat bridge inspired by BRForgers/DisFabric

More information + Docs: [MCDiscordChat Docs | Xujiayao's Blog](https://blog.xujiayao.top/posts/4ba0a17a/)

## Why Fork

I understand the need for users to update to most recent version of a project, but not providing an option to opt out can become a hinderence. If someone doesn't want to update to that should be their decision.

## Introduction

[MCDiscordChat](https://github.com/Xujiayao/MCDiscordChat) (abbreviated as MCDC), a practical and powerful Fabric and Quilt Minecraft <> Discord chat bridge inspired by BRForgers/DisFabric.

![0001.png](https://cdn.jsdelivr.net/gh/Xujiayao/BlogSource@master/source/file/posts/4ba0a17a/0001.png)

![001.png](https://cdn.jsdelivr.net/gh/Xujiayao/BlogSource@master/source/file/posts/4ba0a17a/001.png)

## Features

- Support multi-server mode (multi-server operation on the same Discord channel)
- Support multiple languages (English / Chinese)
- Support displaying server status using Discord channel topic feature
- Minecraft <> Discord cross server chat
  - Support Discord Webhook feature
    - Customizable Webhook Avatar API
    - Use the bot itself to send chat messages when Webhook URL is not filled
  - Support in-game Markdown parsing
  - Support highlighting and using default Unicode and server custom emoji in-game
  - Support highlighting stickers in-game
  - Support highlighting and mentions (@) in-game
    - Support disabling mentions (@) in-game
  - Support highlighting and opening hyperlinks and GIFs in-game
  - Support disabling all parsing
  - Support in-game display of Discord user role colour
  - Support in-game display of response messages
  - Broadcast player command execution
    - Players who execute commands can also see the commands themselves
    - Exclude broadcasts for execution of specific commands
  - Send all console log messages to Discord
- Server Commands available
  - Use Discord to manage channels that can execute MCDC commands
  - Notify in-game players when someone executes an MCDC command
  - Normal Commands
    - /info                    | Query server running status
    - /help                    | Get a list of available commands
    - /update                  | Check for update
    - /stats \<type\> \<name\> | Query the scoreboard of a statistic
  - Admin Commands
    - /reload                  | Reload MCDiscordChat config file (admin only)
    - /console \<command\>     | Execute a command in the server console (admin only)
    - /log                     | Get the specified server log (admin only)
    - /stop                    | Stop the server (admin only)
- Fully customizable message format
  - Sending specific server messages to Discord can be disabled
  - In-game
    - Chat messages from Discord
    - Response messages from Discord
    - Messages from other servers
  - Discord
    - Server started
    - Server stopped
    - Player joined server
    - Player left server
    - Player reached a progress / achieved a goal / completed a challenge
    - Player died
    - Server MSPT is higher than a certain value
- Use admin list to configure user permissions to use special commands
- Support Hot Reloading of the config file
  - Backup every time the config file is loaded
- Check for updates regularly
  - Customizable update notification channel

## Contributors

[![Contributors](https://contrib.rocks/image?repo=xujiayao/mcdiscordchat)](https://github.com/Xujiayao/mcdiscordchat/graphs/contributors)
