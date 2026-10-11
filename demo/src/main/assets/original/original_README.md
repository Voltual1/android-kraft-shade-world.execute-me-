# 目录说明

这个目录存放**委托方提供的原始素材**，用于归档与完整性校验。

它们从收到起**从未被修改**：三份文件的修改时间（2026-08-21 / 2026-08-22 / 2026-09-10）
全部早于本次创作（2026-09-11），SHA-256 见 `../ATTRIBUTION.md`。

| 文件 | 内容 |
| --- | --- |
| `Mili - world.execute (me) ;.mp3` | 歌曲本体。全片唯一的时间基准 |
| `world.execute (me) ;.mid` | 130 BPM 钢琴轨，615 个音符。全片卡点的来源 |
| `world.execute(me)-timeline.lrc` | 131 条歌词时间轴。逐字节内嵌在 `index.html` 中 |

## 让作品跑起来

`index.html` 通过**同级目录的相对路径**引用这些文件。所以要让作品直接运行，
请把 mp3 放在与 `index.html` 同级的位置：

```powershell
Copy-Item "original\Mili - world.execute (me) ;.mp3" .
```

`.mid` 与 `.lrc` 的内容已经解析并嵌入代码（`EM.ONSETS` 与 `index.html` 的
`<script id="lrc-source">`），运行时不需要读取它们 —— 放在这里是为了留下证据链：
任何人都可以拿原始文件去验证作品里的数据是不是真的源自它们。

## 注意文件名里的空格

原始文件名有两处极易在重命名时被吃掉的空格：

```
Mili - world.execute (me) ;.mp3
        ↑              ↑ ↑
        |              | └─ ";" 之前有一个空格
        └─ "execute" 与 "(me)" 之间有一个空格
```

作品会依次尝试几种常见拼写（见 `src/60_app.js` 的 `CLIP_CANDIDATES`），
但首选始终是与这里完全一致的那个名字。
