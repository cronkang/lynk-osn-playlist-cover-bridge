# 原版 USB 封面的获取与展示

以下为原版 USB 媒体接口与封面处理的静态分析总结，用于说明 0.3.2 的适配方式，不包含 OEM 反编译代码。不同固件的具体实现可能不同。

```text
音频内嵌图片
 → MediaProvider / MediaScanner.extractAlbumArt(FileDescriptor)
 → 缩略图生成、保存及 album_art 数据库记录
 → USB 后台按歌曲 album_id 查专辑图片路径
 → MusicBean.albumArt
 → MediaData.albumCoverUri
 ├─ 中控：decodeFile / TIFF fallback → Bitmap → Glide → ImageView
 └─ 仪表：优先 MediaData.albumCover Bitmap，否则 Glide 加载路径
```

## 原版获取

MediaProvider 的 getCompressedAlbumArt 调用系统 MediaScanner 提取内嵌图；makeThumbInternal 按资源限定尺寸进行降采样，必要时使用 RGB_565 解码。writeAlbumArt 可写原压缩字节，也可将 Bitmap 以 JPEG 质量 85 写入。输出目录包含 `Android/data/com.android.providers.media/albumthumbs`，并维护数据库记录、通知专辑 URI 改变。没有在本文确定固定像素尺寸，也不声称每次插盘都会同步执行该方法。

USB MediaDataThread 从 MediaStore 音频记录获取 album_id，批量查询 `content://media/external/audio/albums` 的 `_id` 和 `album_art` 后建立映射，再填充歌曲 albumArt。部分分页/增量路径先给空封面，因此“已收到列表”不一定意味着图片已经就绪。

## 原版展示

中控的 UsbFragment、播放详情和 UsbPlayingFileAdapter 调用 getAlbumThumbBitmap(albumCoverUri)：BitmapFactory.decodeFile 失败后尝试 TIFF 解码，再使用 Glide 显示。列表使用圆角、默认封面与错误占位。字段名称含 Uri，但这条中控路径实际要求可读文件路径，不能直接假定支持 HTTP 或任意 content URI。

UsbAudioUtil 确有 MediaMetadataRetriever.getEmbeddedPicture 工具方法；已检查的 UI 调用点未使用它作为上述流程的回退，不能据此声称主页面会直接从每首音频重新提取图片。

仪表 MediaModel 复制 Bitmap 和 URI。MediaBindAdapter 优先 Bitmap，其次 URI/路径，设置 centerCrop、禁用内存和磁盘缓存；部分图片控件另有路径与 tag 相同就提前返回的判断。覆盖同一路径的文件内容可能不刷新，禁用 Glide 缓存不能消除此提前返回。

## 验证版差异

验证版不运行 MediaProvider 扫描，也不把 MIDI 当作内嵌封面来源。Track.demo 直接生成不同编号的五张 PNG 和 Bitmap。每曲同时具有：

- 本 App 音频文件路径，作为 id 与 uri。
- 本 App 外部文件目录的独立 PNG 路径，作为 albumArt/albumCoverUri。
- 96×96 Bitmap，作为当前曲和 typed 列表的 albumCover。

原版 JSON/PFD 列表只携带图片路径；typed 列表可直接传 Bitmap。后者能覆盖支持该回调的仪表加载路径，但不能证明所有客户端消费它。中控 decodeFile 路径仍需跨进程文件访问权限，必须实车验证。

