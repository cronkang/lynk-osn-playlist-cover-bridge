#OSN 仪表歌曲列表与专辑封面桥接

**OSN Playlist & Cover Bridge**

面向特定 OneOS / Android 9 车机的最小实验 App：通过临时 USB 媒体服务代理，向原车媒体客户端提供五首测试歌曲的播放列表、当前曲、封面、播放状态和进度，并用本应用的 `MediaPlayer` 播放内置 MIDI。

当前公开源码对应 **0.3.2（versionCode 4）**，包名 `dev.lynk.usbprobe`，minSdk / targetSdk 均为 28。源码许可证：**GPL-3.0-only**；音乐素材单独列于 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

**这是验证原型，不是通用投屏软件，也不是完整的第三方音乐播放器桥接器。** 没有屏幕镜像：仪表读取结构化歌曲数据后，用原车 UI 自行绘制。没有 Root、系统 APK 替换、QQ 包名/签名伪装；但会临时改变共享媒体服务表项，能否调用相关接口取决于车机版本与权限。

## 当前版本：0.3.2

- 五首 MIDI 测试音乐、完整歌曲列表、当前曲和五张独立编号封面。
- JSON/PFD 与携带 Bitmap 的 typed 列表两种数据传递方式。
- 应用来源同步、歌曲 ID 统一，以及播放状态和进度回调。
- 手动恢复、主进程超时恢复、独立恢复守护与卸载自身入口。

运行依赖特定 OneOS 媒体服务和 Binder 契约，不是面向所有车型或固件的通用方案。离线构建不等于车机兼容性保证；使用前请阅读当前版本限制。

## 从歌曲数据到仪表显示：完整实现流程

这里的“注册服务”是将本应用提供的 **Binder 接口引用**临时登记到 OneOS 媒体服务表，不是把 APK 上传进原车服务，也不是安装或替换原厂播放器。APK 正常安装后，由应用进程构造数据和代理接口；原车客户端通过这些接口取数据，再绘制自己的界面。

以下分为原车 USB 的数据链路与本项目的代理链路。原车部分是对已研究样本的静态分析，具体固件的服务实现、权限和页面布局可能不同。

### 1. MultiMedia 是界面，USB 播放队列由后台维护

原车 USB 的逻辑数据流是：

```text
U 盘中的音乐文件
    ↓ 系统媒体扫描，提取歌曲、专辑信息和内嵌封面
MediaProvider / MediaStore
    ↓ 歌曲记录、album_id 与 album_art 图片路径
UsbMusicService / UsbManagerImpl
    ↓ 维护 currentPlayList、当前曲和播放状态
IUsbService.getCurrentPlayList() / IUsbListener 通知
    ↓
MediaCenter：UsbMusicPlayer / UsbMusicController
    ↓ 将 MusicBean 转换为统一 MediaData，或转发列表描述符
OneOS 媒体接口：IMediaCenter → IMusicManager
    ↓ 客户端 SDK 的 UsbMusicManager 查询、解析和监听
    ├─ MultiMedia：中控歌曲列表、封面和播放控制
    └─ DimMenu：仪表媒体模型、歌曲列表和封面界面
```

`UsbMusicManager` 是客户端 SDK 的 USB 管理器封装，并不是另一个独立 APK 服务。`MultiMedia` 和 `DimMenu` 分别读取共享媒体接口；不是 MultiMedia 将自己的画面或图片再投给 DimMenu。

USB 列表可以提供多首歌曲的封面数据；仪表是否显示单封面、列表缩略图或相邻歌曲卡片，取决于原车页面实现。不能仅凭三张卡片就认定它们一定对应上一首、当前和下一首。

### 2. 涉及哪些车机服务和接口

| 组件 | 包名、接口或类 | 作用与调用关系 |
|---|---|---|
| OneOS 服务入口 | `com.geely.service.oneosapi.OneOSApiService` | 本项目显式绑定的 Android 服务，所在包为 `com.geely.service.oneosapi` |
| OneOS 服务表接口 | `com.geely.lib.oneosapi.IServiceManager` | 通过 `getService(3)` 取得媒体中心引用，通过 `addService(3, binder)` 登记代理 |
| 原媒体中心 | `com.geely.mediacenterservice` / `IMediaCenter` | 提供音乐接口、当前音源、应用来源及音源请求 |
| 音乐接口 | `IMusicManager` | 按音源提供队列、当前曲、状态、进度和播放控制 |
| 原 USB 后台 | `com.geely.usbservice` / `IUsbService` | 原车链路的队列维护与 USB 播放后台，由媒体中心连接 |
| 客户端 SDK | `MediaCenterManager`、`UsbMusicManager`、`ResolveUtil` | 将应用层查询转换为 Binder 调用，并把 JSON 列表转成 MediaData |
| 中控客户端 | MultiMedia，包名 `com.tencent.wecarflow` | USB 界面、播放列表和封面消费端 |
| 仪表客户端 | DimMenu，包名 `com.geely.dimmenu` | 将统一媒体数据转换为仪表模型并展示 |
| 本项目 | `dev.lynk.usbprobe` | 提供测试队列、代理 Binder、MIDI 播放和恢复机制 |

接口完整描述符包括：

```text
com.geely.lib.oneosapi.IServiceManager
com.geely.lib.oneosapi.mediacenter.IMediaCenter
com.geely.lib.oneosapi.mediacenter.IMusicManager
com.geely.lib.oneosapi.mediacenter.listener.IMediaStateListener
com.geely.lib.oneosapi.mediacenter.listener.IMusicStateListener
com.geely.lib.oneosapi.mediacenter.callback.IMusicListCallback
```

`getService(3)` 中的 **3 是服务表中的媒体服务标识**，不是 USB 音源编号，也不是 Binder 交易编号。本项目使用的 **USB 音源编号是 1**。例如该协议快照中的 `getService` 交易编号为 2，`addService` 为 1；它们与参数中的服务标识属于不同概念。

0.3.2 不直接向 `IUsbService` 写入虚拟歌曲，也不修改 MediaProvider 数据库；其入口是 OneOS 服务表和统一媒体接口。

### 3. 本项目先构造五首歌曲及各自的封面

`ProbeService.onCreate()` 调用 `Track.demo()`：

```text
assets/track-1.mid … track-5.mid
    ↓ 复制到应用私有 files 目录
五个音频文件路径
    ↓ 构造五个 Track
每项含标题、歌手、专辑、时长、歌曲标识
    ↓ 按序号绘制 96×96 Bitmap
五张不同颜色的编号封面
    ↓ 保存为外部应用目录中的 covers/cover-N.png
每首同时拥有 Bitmap 和图片文件路径
```

关键字段关系：

| 本项目字段 | 对外数据 | 用途 |
|---|---|---|
| `id = uri = 音频绝对路径` | JSON 的 `path`、MediaData 的 `id/uri` | 让当前曲、列表项和点选命令定位同一首歌 |
| `name / artist / album` | `name / artist / albumName` | 标题、歌手和专辑文字 |
| `duration` | long，单位毫秒 | 实际播放准备完成后更新为播放器时长 |
| `art` | JSON 的 `albumArt`、MediaData 的 `albumCoverUri` | 供接收进程加载的 PNG 路径 |
| `bitmap` | MediaData 的 `albumCover` | 直接通过 Parcel 传递封面像素 |
| `source = 1` | MediaData 的 USB 来源 | 区分 USB 与其他音源 |

这些 MIDI 不依赖内嵌封面。编号图片由应用直接绘制，不需要先经过原 USB 扫描流程。音频在本包内播放，原车 UI 不负责解码这些私有 MIDI 文件。

### 4. 绑定 OneOS，取得并保存原媒体服务

应用通过 `bindService()` 显式绑定 OneOSApiService，取得 registry Binder 后，先核对 descriptor，再进行原服务检查：

```text
ProbeService
    → 绑定 OneOSApiService
    → registry.getService(3)
    ← original：原 IMediaCenter Binder
    → original.getMusicManager()
    ← music：原 IMusicManager Binder
    → 查询 getCurrentAudioSource / getCurrentAppSource
    → 查询原 USB 当前曲与队列
```

绑定成功只说明取得了服务入口，不代表已经允许注册代理；原接口描述符和可读性检查也需要通过。只读检查阶段不会替换媒体服务。

本项目使用 `Parcel.writeInterfaceToken()`、协议交易编号和 `Binder.transact()` 调用接口。方法表在 `Contracts.java` 和 `protocol/contracts.json` 中，六组共 115 项；普通构建不需要原厂 SDK 或 APK。这里保存的是特定协议快照，不是所有固件通用的接口标准。

### 5. 先启动独立恢复守护，再注册两层媒体代理

启用前，主进程将以下引用交给运行在 `:recovery` 进程的 `RecoveryService`：

- OneOS registry Binder。
- 原媒体服务 original Binder。
- 待注册的 proxy Binder。
- 代表主进程存活的 life Binder。

恢复进程保存引用，监听主进程 Binder 死亡，并设置约 185 秒超时。主进程确认原服务引用尚未变化后再注册：

```text
registry.addService(3, MediaProxy)
    ↓
registry.getService(3) 回读比较
    ↓
确认表项指向本应用 MediaProxy
```

这一步登记的是一个进程间接口，不是上传 APK 或重新安装系统服务。车机权限策略必须允许相应调用，不能将当前实现视为任意系统上的权限绕过方式。

两层代理的路由关系：

```text
原车客户端
    → MediaProxy（替代服务表中的媒体中心引用）
        ├─ getMusicManager() → 返回 MusicProxy
        └─ 其他根接口调用 → 转交 original

MusicProxy
    ├─ 指定 USB 查询、监听和控制 → 由本项目处理
    └─ 非 USB / 其他音乐方法 → 转交原 music 接口
```

因此项目只为指定 USB 分支提供测试数据，其他调用尽量透传。已有缓存原 Binder 的客户端可能不会立即取得代理；服务表回读成功不等于仪表已经接入。

### 6. 客户端如何拿到完整列表：查询与监听两条入口

客户端取得 `MusicProxy` 后，可以主动查询，也可以登记监听器等待推送。

| 方法 | 本项目返回或处理方式 |
|---|---|
| `getCurrentMediaData(USB)` | 当前 Track，包含 Bitmap 和图片路径 |
| `getPlayList(USB)` | 五项 typed MediaData 列表 |
| `getUsbPlayList(USB)` | 包含五首歌曲 JSON 的 ParcelFileDescriptor |
| `getPlayListAsync(USB, callback)` | 通过列表回调返回 JSON/PFD |
| `addMediaStateListener` / `addMusicStateListener` | 保存客户端监听 Binder，延迟触发推送 |
| `getCurrentPlayState` / `getCurrentPosition` | 本应用播放状态与位置 |

**JSON/PFD 路径：**

```text
List<Track>
    → JSON：path/title/artist/album/duration/albumArt 等字段
    → 写入临时文件并打开只读 ParcelFileDescriptor
    → 跨进程交给客户端
    → ResolveUtil 读取 JSON 并还原歌曲列表
    → path 映射为 id/uri，albumArt 映射为 albumCoverUri
```

原后台使用 MemoryFile；验证 App 使用普通临时文件承载字节。二者实现不同，但客户端消费的都是描述符内容。JSON 不含 Bitmap，所以该分支需要接收进程成功读取图片路径。

**typed MediaData 路径：**

```text
List<Track>
    → 按原 MediaData Parcel 顺序逐项写入
    → writeTypedList：五首歌曲，每项带 Bitmap 和路径
    → 客户端接收统一媒体列表
```

字段写入顺序为：

```text
id, artist, name, duration, uri,
albumCover, albumCoverUri, albumName, source,
isFavored, mediaType, isLrcSupported,
isReplaySupported, isFavorSupported, vip
```

顺序、类型和 Parcelable 存在标志必须匹配，不能只使用同名 JSON 字段来替代 Parcel 协议。

### 7. 应用来源、当前曲、状态和进度怎样推送

`publish()` 区分媒体监听与音乐监听：前者带 `audioSource + appSource`，后者带 `audioSource`。歌曲或时长变化时按顺序调用：

```text
onUsbPlayListChanged    → JSON/PFD 完整列表
onPlayListChanged       → 含五张 Bitmap 的 typed 完整列表
onMediaDataChanged      → 当前曲
onPlayStateChanged      → 播放/暂停状态
onPlayPositionChanged   → 当前进度与时长
```

启用后调用原媒体中心 `requestAudioSource(USB=1, UNKNOWN=0)`，并回读真实音源。回调初始 appSource 为 1；当真实音源为 USB 时，再用 `getCurrentAppSource()` 同步实际应用来源。音源、应用来源和歌曲 ID 是三个不同字段；来源不被页面支持时，回调可能被过滤。

约每 2 秒心跳更新状态与进度；同一监听器的歌曲和时长未变时，不在每个心跳重发全部列表。日志 D1/D2 表示回调调用返回，不表示客户端一定接受或成功渲染。

### 8. 中控和仪表怎样加载、显示封面

原版封面的获取路径是：

```text
音频内嵌图片
    → MediaProvider 提取并生成缩略图
    → 专辑数据库 album_art 路径
    → MusicBean.albumArt
    → MediaData.albumCoverUri
```

验证 App 直接生成上述消费端需要的 Bitmap 与文件路径。数据送达后，中控和仪表分别加载：

```text
MultiMedia
    → UsbFragment / UsbPlayingFileAdapter
    → getAlbumThumbBitmap(albumCoverUri)
    → BitmapFactory.decodeFile，失败时尝试 TIFF 解码
    → Glide 加载 Bitmap 到 ImageView

DimMenu
    → MediaInteractiveManager 接收媒体数据
    → MediaModel 保存当前媒体字段
    → MediaBindAdapter
    → 优先 albumCover Bitmap，否则加载 albumCoverUri
    → 原车仪表页面绘制封面、文字和相关状态
```

只传 Bitmap 不足以覆盖中控的文件解码入口；只传路径则依赖接收进程权限。因而项目同时提供二者。不同歌曲使用独立 PNG 路径，避免混淆；部分仪表控件会在路径与旧 tag 相同时跳过刷新，同路径覆盖图片内容也可能显示旧图。

完整列表、当前曲和图片三者都到达客户端，仍不保证原车界面会显示多个封面。页面布局与数据消费入口由车机控制；此项目不修改仪表页面，也不直接操纵显示硬件。

### 9. 点选歌曲、上下曲和音频如何形成闭环

```text
仪表列表点选 / 上一首 / 下一首
    → UsbMusicManager / IMusicManager
    → MusicProxy.playItem / prev / next
    → 按 id 或 uri 匹配 Track 并更新 index
    → MediaPlayer 播放对应 MIDI
    → publish() 推送列表、当前曲、状态和进度
    → 仪表重新显示对应歌曲与封面
```

应用自己的五个歌曲按钮直接发送 select 动作，后续走相同的选曲和推送逻辑。暂停、继续和 seek 映射到本应用播放器。某些中控 `PLAY_*` 字符串命令只记录，未实现对应操作，不能认为所有控制入口都等价。

音频由本包 `MediaPlayer` 输出，媒体数据通过代理输出，两者不是同一条通道。播放器先申请 Android 音频焦点，焦点丢失时暂停；启用时先申请 USB 音源，再延迟约 1200ms 尝试启动音频。

### 10. 测试结束后怎样恢复

手动停止、主进程约 180 秒超时、观察到音源离开 USB 或服务引用变化时，应用进入恢复流程：

```text
停止 MediaPlayer 与心跳
    ↓ 读取当前服务表项
仍为本代理 → addService(3, original) → 回读确认
已经变化   → 保留现有引用，不强行覆盖
    ↓ 尽力把已记录监听器重新登记到原媒体服务
解除恢复守护、结束前台测试服务
```

独立恢复进程在主进程死亡或约 185 秒超时时另行尝试恢复。卸载按钮先请求恢复，再由 Activity 打开系统卸载确认界面。恢复针对原服务 Binder 引用，不等于恢复之前的音源、歌曲和播放进度；整包强停、断电等情况不保证守护能运行。

### 11. 读源码时从哪里开始

建议按以下顺序阅读：

1. `Track.demo/json/writeToParcel`：了解五首数据和封面如何编码。
2. `ProbeService.diagnose/attemptEnable`：了解原接口检查与代理登记。
3. `MediaProxy/MusicProxy.onTransact`：了解查询路由、监听登记与控制。
4. `ProbeService.publish/tick`：了解列表、当前曲、状态和进度推送。
5. `ProbeService.restore` 与 `RecoveryService`：了解条件恢复及守护。
6. `MainActivity`、`Support`、`Contracts`：了解操作入口、Binder 编码辅助和方法编号。

扩展真实播放器时，应将固定 Track 数据源替换为目标播放器公开的真实队列、当前曲和封面，并将控制回送该播放器。只有当前曲通知时无法推导真实完整列表；还需验证目标播放器是否提供足够的数据与控制接口。

## 项目文档

| 文档 | 内容 |
|---|---|
| [完整实现流程](docs/ARCHITECTURE.md) | 服务接入时序、代理路由、列表编码、封面推送、控制与恢复 |
| [封面获取与展示](docs/ALBUM_ART.md) | 原版 USB 封面来源、路径与 Bitmap 的区别，以及仪表加载逻辑 |
| [构建指南](docs/BUILD.md) | 开发环境、编译命令、输出与签名 |
| [当前版本说明](docs/RELEASE_NOTES_0.3.2.md) | 0.3.2 功能与安装包信息 |
| [已知限制](docs/KNOWN_LIMITATIONS.md) | 固件兼容性、文件权限、生命周期与恢复边界 |

## 下载与测试

安装包：`USB-Instrument-Probe-0.3.2.apk`。下载文件后可按[版本说明](docs/RELEASE_NOTES_0.3.2.md)中的 SHA-256 校验；源码需按构建指南编译后才能安装。

1. 停车，调低音量，拔掉 U 盘，暂停其他播放器，安装并打开 App。
2. 点“只读检查 OneOS 和原车 USB”，确认接口可读；错误时先导出日志。
3. 点“无U盘启动五首USB音乐（3分钟）”。观察 B1 服务表回读、S2 原服务真实音源与 P1 音频启动。
4. 观察仪表列表、当前封面、状态和进度；选第 3 首便于观察可能的前后项。分别测试应用按钮、仪表列表点选和上下曲，记录实际控制入口。
5. 完成后点“立即恢复原车媒体服务”，检查 R2。需要反馈时先“导出完整日志”。
6. 可点“卸载自身（先停止测试并恢复）”。应用尝试停止测试并恢复后打开系统卸载确认；用户仍需在系统界面确认。

测试约 180 秒自动恢复，独立进程约 185 秒尝试恢复。测试期间不要强停、直接卸载或使用清理器结束整个包；恢复守护也会被整包强停，不能保证断电或服务异常时恢复。恢复的是媒体服务 Binder 引用，不等于恢复之前歌曲、进度或音源。更多限制见 [KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md)。

## 源码目录

```text
AndroidManifest.xml             包配置与双进程服务声明
src/dev/lynk/usbprobe/
  MainActivity.java             测试界面、日志导出、卸载入口
  ProbeService.java             代理注册、USB 数据与控制、音频播放、恢复
  RecoveryService.java          独立恢复进程、Binder 死亡与超时处理
  Track.java                    五首数据、96×96 封面、Parcel 与 JSON 编码
  Support.java                  Binder 调用、前台通知与日志
  Contracts.java                已核对的接口方法编号表
protocol/contracts.json         六组 115 项编号和 MediaData 字段顺序
assets/                         五首 MIDI 及来源、校验值
verification/                   0.3.2 APK 信息与源码构建一致性检查
build.py                        独立构建；无需原厂 APK 或第三方 Python 包
```

源码对应 0.3.2，包含六个应用 Java 文件、接口编号表及 Manifest。构建脚本不依赖原厂 APK。

0.3.2 的构建一致性记录见 [source-export-check.json](verification/source-export-check.json)：对应构建中的 DEX、二进制 Manifest 和五首 MIDI 与发布 APK 一致。不同工具链的输出、签名和整个 APK 文件不保证逐字节一致。

不包含原厂 APK / DEX / SDK 实现、反汇编全文、原始实车日志、账号资料或签名私钥。接口表是互操作所需的方法名称/编号快照，不是跨固件通用的官方 SDK。

## 反馈

请附上 Android / OneOS 版本、测试入口、单封面还是多封面、是否有进度、是否能恢复，以及删去个人内容后的诊断日志。D1 表示推送调用返回，不代表仪表已经显示；提供观察结果才能确认显示成功。

## 许可

Copyright (C) 2026 minyu.

本项目原创源码与文档依据 GNU General Public License version 3 only 发布，完整条款见 [LICENSE](LICENSE)。第三方 MIDI 素材按其来源声明处理，不因本项目而改换许可。

本项目旨在供个人学习与技术研究参考，源码使用与分发遵循 GPL-3.0 许可证。如认为本项目涉及侵权，请联系作者核实处理，必要时删除相关内容。
