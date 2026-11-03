package dev.lynk.usbprobe;
final class Contracts {
static int code(String group,String name){
if(group.equals("registry")){
if(name.equals("addService")) return 1;
if(name.equals("getPermissionService")) return 3;
if(name.equals("getService")) return 2;
if(name.equals("registerCallback")) return 4;
if(name.equals("unregisterCallback")) return 5;
}
if(group.equals("media")){
if(name.equals("addPsdSourceStateListener")) return 9;
if(name.equals("addRebootPlayChangeListener")) return 23;
if(name.equals("addSourceStateListener")) return 4;
if(name.equals("getAppPackageName")) return 20;
if(name.equals("getCarManager")) return 7;
if(name.equals("getCurrentAppSource")) return 2;
if(name.equals("getCurrentAudioSource")) return 1;
if(name.equals("getCurrentPsdAppSource")) return 13;
if(name.equals("getCurrentPsdAudioSource")) return 12;
if(name.equals("getFocusedAppSource")) return 17;
if(name.equals("getFocusedAudioSource")) return 16;
if(name.equals("getMusicManager")) return 5;
if(name.equals("getPsdFocusedAppSource")) return 19;
if(name.equals("getPsdFocusedAudioSource")) return 18;
if(name.equals("getRadioManager")) return 6;
if(name.equals("getRebootPlay")) return 22;
if(name.equals("isPsdBtAudioSource")) return 10;
if(name.equals("isPsdBtDeviceConnected")) return 15;
if(name.equals("requestAudioSource")) return 3;
if(name.equals("setHeartbeatPacket")) return 8;
if(name.equals("setPsdBtAudioSource")) return 11;
if(name.equals("setPsdBtChannel")) return 14;
if(name.equals("setRebootPlay")) return 21;
}
if(group.equals("music")){
if(name.equals("addDeviceStateListener")) return 2;
if(name.equals("addFavor")) return 12;
if(name.equals("addMediaStateListener")) return 52;
if(name.equals("addMusicStateListener")) return 1;
if(name.equals("addPsdMediaStateListener")) return 53;
if(name.equals("addPsdMusicStateListener")) return 44;
if(name.equals("cancelFavor")) return 13;
if(name.equals("connectBtDevice")) return 24;
if(name.equals("deleteThirdPartyAccountBindState")) return 64;
if(name.equals("fastForward")) return 9;
if(name.equals("fastRewind")) return 10;
if(name.equals("getBtState")) return 30;
if(name.equals("getConnectedDevice")) return 29;
if(name.equals("getContent")) return 35;
if(name.equals("getCurrentMediaBundle")) return 51;
if(name.equals("getCurrentMediaData")) return 17;
if(name.equals("getCurrentPathFiles")) return 23;
if(name.equals("getCurrentPlayState")) return 15;
if(name.equals("getCurrentPosition")) return 16;
if(name.equals("getCurrentUsbPathFiles")) return 58;
if(name.equals("getDevicesInfo")) return 18;
if(name.equals("getFavorList")) return 14;
if(name.equals("getLoginQrCodeAsync")) return 62;
if(name.equals("getOnlineUserInfoAsync")) return 26;
if(name.equals("getOnlineUserVip")) return 69;
if(name.equals("getPlayList")) return 11;
if(name.equals("getPlayListAsync")) return 43;
if(name.equals("getPlayMode")) return 21;
if(name.equals("getRecentlyAppSource")) return 34;
if(name.equals("getUsbPlayList")) return 59;
if(name.equals("isAgreedUserProtocol")) return 55;
if(name.equals("isLogin")) return 61;
if(name.equals("isMusicQualitySwitch")) return 56;
if(name.equals("isSupportChangeMode")) return 54;
if(name.equals("logout")) return 67;
if(name.equals("next")) return 7;
if(name.equals("notifyVrStatusNotifierStatus")) return 57;
if(name.equals("onUIWordingTriggered")) return 50;
if(name.equals("openFavor")) return 32;
if(name.equals("openHistory")) return 33;
if(name.equals("openLoginPage")) return 45;
if(name.equals("openLrcPage")) return 47;
if(name.equals("openPlayList")) return 38;
if(name.equals("pause")) return 5;
if(name.equals("play")) return 4;
if(name.equals("playContent")) return 36;
if(name.equals("playFavor")) return 39;
if(name.equals("playItem")) return 3;
if(name.equals("playSearchSongByNameItem")) return 28;
if(name.equals("prev")) return 6;
if(name.equals("queryThirdPartyAccountBindState")) return 65;
if(name.equals("queryThirdPartyAccountID")) return 68;
if(name.equals("replay")) return 46;
if(name.equals("searchMediaForVR")) return 37;
if(name.equals("searchMusic")) return 60;
if(name.equals("searchSongByName")) return 27;
if(name.equals("searchSongByNameAsync")) return 31;
if(name.equals("seekTo")) return 8;
if(name.equals("semanticSearch")) return 41;
if(name.equals("semanticSearchAndPlay")) return 48;
if(name.equals("semanticSearchAsync")) return 42;
if(name.equals("sendCommand")) return 49;
if(name.equals("setActiveBtDevice")) return 25;
if(name.equals("setPlayMode")) return 20;
if(name.equals("startApp")) return 19;
if(name.equals("startAppTab")) return 40;
if(name.equals("startScanPath")) return 22;
if(name.equals("thirdPartyAccountLogin")) return 66;
if(name.equals("updateThirdPartyAccount")) return 63;
}
if(group.equals("list")){
if(name.equals("onPlayListChanged")) return 1;
if(name.equals("onUsbPlayListChanged")) return 2;
}
if(group.equals("state")){
if(name.equals("onFavorStateChanged")) return 5;
if(name.equals("onLrcLoad")) return 6;
if(name.equals("onMediaDataChanged")) return 1;
if(name.equals("onPlayListChanged")) return 4;
if(name.equals("onPlayModeChange")) return 7;
if(name.equals("onPlayPositionChanged")) return 2;
if(name.equals("onPlayStateChanged")) return 3;
if(name.equals("onUsbPlayListChanged")) return 8;
}
if(group.equals("musicState")){
if(name.equals("onFavorStateChanged")) return 5;
if(name.equals("onLrcLoad")) return 6;
if(name.equals("onMediaDataChanged")) return 1;
if(name.equals("onPlayListChanged")) return 4;
if(name.equals("onPlayModeChange")) return 7;
if(name.equals("onPlayPositionChanged")) return 2;
if(name.equals("onPlayStateChanged")) return 3;
if(name.equals("onUsbPlayListChanged")) return 8;
}
throw new IllegalArgumentException(group+"/"+name);
}
static String name(String group,int code){
if(group.equals("registry")){switch(code){
case 1: return "addService";
case 3: return "getPermissionService";
case 2: return "getService";
case 4: return "registerCallback";
case 5: return "unregisterCallback";
}}
if(group.equals("media")){switch(code){
case 9: return "addPsdSourceStateListener";
case 23: return "addRebootPlayChangeListener";
case 4: return "addSourceStateListener";
case 20: return "getAppPackageName";
case 7: return "getCarManager";
case 2: return "getCurrentAppSource";
case 1: return "getCurrentAudioSource";
case 13: return "getCurrentPsdAppSource";
case 12: return "getCurrentPsdAudioSource";
case 17: return "getFocusedAppSource";
case 16: return "getFocusedAudioSource";
case 5: return "getMusicManager";
case 19: return "getPsdFocusedAppSource";
case 18: return "getPsdFocusedAudioSource";
case 6: return "getRadioManager";
case 22: return "getRebootPlay";
case 10: return "isPsdBtAudioSource";
case 15: return "isPsdBtDeviceConnected";
case 3: return "requestAudioSource";
case 8: return "setHeartbeatPacket";
case 11: return "setPsdBtAudioSource";
case 14: return "setPsdBtChannel";
case 21: return "setRebootPlay";
}}
if(group.equals("music")){switch(code){
case 2: return "addDeviceStateListener";
case 12: return "addFavor";
case 52: return "addMediaStateListener";
case 1: return "addMusicStateListener";
case 53: return "addPsdMediaStateListener";
case 44: return "addPsdMusicStateListener";
case 13: return "cancelFavor";
case 24: return "connectBtDevice";
case 64: return "deleteThirdPartyAccountBindState";
case 9: return "fastForward";
case 10: return "fastRewind";
case 30: return "getBtState";
case 29: return "getConnectedDevice";
case 35: return "getContent";
case 51: return "getCurrentMediaBundle";
case 17: return "getCurrentMediaData";
case 23: return "getCurrentPathFiles";
case 15: return "getCurrentPlayState";
case 16: return "getCurrentPosition";
case 58: return "getCurrentUsbPathFiles";
case 18: return "getDevicesInfo";
case 14: return "getFavorList";
case 62: return "getLoginQrCodeAsync";
case 26: return "getOnlineUserInfoAsync";
case 69: return "getOnlineUserVip";
case 11: return "getPlayList";
case 43: return "getPlayListAsync";
case 21: return "getPlayMode";
case 34: return "getRecentlyAppSource";
case 59: return "getUsbPlayList";
case 55: return "isAgreedUserProtocol";
case 61: return "isLogin";
case 56: return "isMusicQualitySwitch";
case 54: return "isSupportChangeMode";
case 67: return "logout";
case 7: return "next";
case 57: return "notifyVrStatusNotifierStatus";
case 50: return "onUIWordingTriggered";
case 32: return "openFavor";
case 33: return "openHistory";
case 45: return "openLoginPage";
case 47: return "openLrcPage";
case 38: return "openPlayList";
case 5: return "pause";
case 4: return "play";
case 36: return "playContent";
case 39: return "playFavor";
case 3: return "playItem";
case 28: return "playSearchSongByNameItem";
case 6: return "prev";
case 65: return "queryThirdPartyAccountBindState";
case 68: return "queryThirdPartyAccountID";
case 46: return "replay";
case 37: return "searchMediaForVR";
case 60: return "searchMusic";
case 27: return "searchSongByName";
case 31: return "searchSongByNameAsync";
case 8: return "seekTo";
case 41: return "semanticSearch";
case 48: return "semanticSearchAndPlay";
case 42: return "semanticSearchAsync";
case 49: return "sendCommand";
case 25: return "setActiveBtDevice";
case 20: return "setPlayMode";
case 19: return "startApp";
case 40: return "startAppTab";
case 22: return "startScanPath";
case 66: return "thirdPartyAccountLogin";
case 63: return "updateThirdPartyAccount";
}}
if(group.equals("list")){switch(code){
case 1: return "onPlayListChanged";
case 2: return "onUsbPlayListChanged";
}}
if(group.equals("state")){switch(code){
case 5: return "onFavorStateChanged";
case 6: return "onLrcLoad";
case 1: return "onMediaDataChanged";
case 4: return "onPlayListChanged";
case 7: return "onPlayModeChange";
case 2: return "onPlayPositionChanged";
case 3: return "onPlayStateChanged";
case 8: return "onUsbPlayListChanged";
}}
if(group.equals("musicState")){switch(code){
case 5: return "onFavorStateChanged";
case 6: return "onLrcLoad";
case 1: return "onMediaDataChanged";
case 4: return "onPlayListChanged";
case 7: return "onPlayModeChange";
case 2: return "onPlayPositionChanged";
case 3: return "onPlayStateChanged";
case 8: return "onUsbPlayListChanged";
}}
return null;
}
}