# claude-config

A House Elves dashboard for viewing and managing Claude Code configuration across all levels.

## Features

- **Permissions viewer** — See all allowed commands across user, user-local, project, and project-local levels with source-level badges
- **Stale permission cleanup** — Detects permissions that look stale (PIDs, /tmp/ paths, hardcoded tokens, date-specific files, one-off commands) and offers bulk cleanup
- **Move between levels** — Easily move permissions and config between user/project/local levels
- **Settings editor** — View and edit settings.json at all 4 levels
- **CLAUDE.md editor** — Side-by-side editing of user and project CLAUDE.md files
- **MCP config editor** — View and edit .mcp.json at user and project levels
- **Keybindings editor** — Edit ~/.claude/keybindings.json
- **Commands manager** — View, edit, create, and delete slash commands at user and project levels
- **Project discovery** — Auto-discovers projects with Claude config under configurable paths
- **Live updates** — WebSocket-based file watching pushes changes when config files are modified externally
- **Dark/light theme** — Matching the github-worker-ui design system

## Running

```shell
./mvnw quarkus:dev
```

Dashboard available at http://localhost:7479

## Configuration

App config at `~/.config/claude-config/config`:

```
PROJECT_PATHS=~/Projects
```

Multiple paths separated by `:` (e.g. `~/Projects:~/Work`).

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/projects` | List discovered projects |
| `GET /api/permissions/merged?project=...` | All permissions with source-level annotation |
| `GET /api/permissions/stale?project=...` | Stale permissions grouped by reason |
| `POST /api/permissions/move` | Move permission between levels |
| `POST /api/permissions/bulk-remove` | Bulk delete stale permissions |
| `GET/PUT /api/settings/{level}` | Read/write settings JSON |
| `GET/PUT /api/claude-md/{level}` | Read/write CLAUDE.md |
| `GET/PUT /api/mcp/{level}` | Read/write .mcp.json |
| `GET/PUT /api/keybindings` | Read/write keybindings.json |
| `GET/PUT/DELETE /api/commands/{level}/{name}` | Manage slash commands |
| `WS /api/live` | WebSocket for live config change notifications |

## Tech Stack

- [Quarkus REST Jackson](https://quarkus.io/guides/rest#json-serialisation)
- [Quarkus WebSockets Next](https://quarkus.io/guides/websockets-next-reference)
- [Quarkus Scheduler](https://quarkus.io/guides/scheduler)
