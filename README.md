# region-investigation

# 配置项(assets/config.json)说明：

* backend - 后台服务器接口前缀，例如http://192.168.0.181/ria
* apiKey - 高德地图api key
* offlineMapDir - 离线地图存储地址，注意一定是存储卡的相对位置, 如果不想冒险，就保持为"autonavi/"，这个是高德官方应用的离线地图位置
* fakeDate - 是否伪造数据，不是开发者，就不要打开这个flag

# 如何启动本应用

```kotlin
val intent = Intent(Intent.ACTION_SEND, Uri.parse("region-investigation://"))
intent.putExtra("USERNAME", "用户名")
intent.putExtra("ORG_NAME", "组织名称")
intent.putExtra("ORG_CODE", "组织代码")
if (intent.resolveActivity(packageManager) != null) {
  startActivity(intent)
}
```
