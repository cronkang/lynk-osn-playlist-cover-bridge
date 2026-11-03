# 素材与接口来源

## MIDI

assets/track-1.mid 至 track-5.mid 来自 Mutopia Project 的 Scott Joplin 曲目：Bethena、The Entertainer、Magnetic Rag、Maple Leaf Rag、Original Rags。

Mutopia 的 [Joplin 列表](https://www.mutopiaproject.org/cgibin/make-table.cgi?collection=joplin&preview=1) 对这五个项目标注 Public Domain。本包使用的是 MIDI 文件，不是第三方录音。每个文件的原始下载 URL、字节数、来源声明及 SHA-256 在 [assets/SOURCES.json](assets/SOURCES.json)。封面为程序绘制的纯色编号，不使用 Mutopia 的乐谱预览图或任何专辑商业图片。

## OEM 接口契约

协议快照记录已研究客户端中的接口方法名称、交易编号和 Parcel 字段布局，以便自有实现互操作；六组共 115 项。源样本仅记录文件名及 SHA-256，不随仓库发布 OEM APK、DEX、SDK 实现或反编译全文。该快照不是 OEM 官方 API 或授权兼容承诺。

## 开发工具

JDK、Android SDK 和 Python 由开发者自行安装，不随仓库打包。公开 build.py 仅使用 Python 标准库。接口分析工具不属于运行时依赖，普通构建无需 Androguard。
