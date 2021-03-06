package xyz.yhsj.kmusic.impl

import xyz.yhsj.json.JSONObject
import xyz.yhsj.khttp.get
import xyz.yhsj.kmusic.entity.MusicResp
import xyz.yhsj.kmusic.entity.MusicTop
import xyz.yhsj.kmusic.entity.Song
import xyz.yhsj.kmusic.utils.future

/**
 * 咪咕音乐
 */
object MiguImpl : Impl {
    /**
     * 根据类型,获取歌曲排行榜详情
     * http://m.10086.cn/migu/remoting/ranking_list_tag?pageSize=20&nid=22296055&pageNo=0
     * http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334476.png
     * 咪咕音乐榜  http://m.10086.cn/migu/remoting/ranking_list_tag?nid=22296055
     * http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334477.png
     * 咪咕影视榜 http://m.10086.cn/migu/remoting/ranking_list_tag?nid=22296096
     * http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334478.png
     * 咪咕网络榜 http://m.10086.cn/migu/remoting/ranking_list_tag?nid=22296137
     * http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334479.png
     * 咪咕原创榜 http://m.10086.cn/migu/remoting/ranking_list_tag?nid=22296178
     * http://m.10086.cn/migu/remoting/ranking_list_tag?pageSize=20&nid=22296055&pageNo=0
     *
     */
    override fun getSongTopDetail(topId: String, topType: String, topKey: String, page: Int, num: Int): MusicResp<List<Song>> {
        return try {
            val resp = get(url = "http://m.10086.cn/migu/remoting/ranking_list_tag?nid=$topId"
                    , headers = mapOf("Referer" to "http://m.10086.cn",
                    "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"
            ))
            if (resp.statusCode != 200) {
                MusicResp.failure(code = resp.statusCode, msg = "请求失败")
            } else {
                val radioData = resp.jsonObject
                val songList = radioData
                        .getJSONObject("result")
                        .getJSONArray("results")
                val songIds = songList.map {
                    (it as JSONObject).getJSONObject("songData").getString("songId")
                }
                getSongById(songIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            MusicResp.failure(msg = e.message)
        }
    }

    /**
     * 获取歌曲排行榜
     */
    override fun getSongTop(): MusicResp<List<MusicTop>> {
        val tops = arrayListOf(
                MusicTop(site = "migu", topId = "22296055", name = "咪咕音乐榜", pic = "http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334476.png", comment = "咪咕音乐榜"),
                MusicTop(site = "migu", topId = "22296096", name = "虾米原创榜", pic = "http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334477.png", comment = "虾米原创榜"),
                MusicTop(site = "migu", topId = "22296137", name = "咪咕影视榜", pic = "http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334478.png", comment = "咪咕影视榜"),
                MusicTop(site = "migu", topId = "22296178", name = "咪咕音乐人", pic = "http://m.10086.cn/migu/fs/media/p/149/163/5112/image/20171227/1334479.png", comment = "咪咕音乐人"))
        return MusicResp.success(data = tops)
    }

    /**@param key 关键字
     * @param page 页数
     * 搜索
     */
    override fun search(key: String, page: Int, num: Int): MusicResp<List<Song>> {
        return try {
            val resp = get(url = "http://m.10086.cn/migu/remoting/scr_search_tag?type=2&rows=$num&keyword=$key&pgc=$page"
                    , headers = mapOf("Referer" to "http://m.10086.cn",
                    "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"
            ))
            if (resp.statusCode != 200) {
                MusicResp.failure(code = resp.statusCode, msg = "请求失败")
            } else {
                val radioData = resp.jsonObject
                val songList = radioData
                        .getJSONArray("musics")

                val songIds = songList.map {
                    (it as JSONObject).getString("id")
                }
                getSongById(songIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            MusicResp.failure(msg = e.message)
        }
    }

    /**
     * 咪咕接口看人品,上面搜索可能出来的音乐,到下面可能获取不到播放地址什么的
     * 根据id获取歌曲，部分接口支持获取多个，id用“，”分开
     */
    override fun getSongById(songIds: List<String>): MusicResp<List<Song>> {
        val songs: List<Song> =
                songIds.future { songId ->

                    try {
                        val songResp = get(url = "http://music.migu.cn/v2/async/audioplayer/playurl/$songId",
                                headers = mapOf("Referer" to "http://m.10086.cn",
                                        "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"))
                        if (songResp.statusCode != 200) {
                            Song(
                                    site = "migu",
                                    code = songResp.statusCode,
                                    msg = "网络异常",
                                    songid = songId)
                        } else {
                            val songInfo = songResp.jsonObject
                            val radioSongId = songInfo.getString("musicId")
                            val radioAuthor = songInfo.getJSONArray("artistInfoList").joinToString(",") {
                                (it as JSONObject).getString("artistName", "")
                            }
                            Song(
                                    site = "migu",
                                    link = "http://music.migu.cn/v2/music/song/$radioSongId",
                                    songid = radioSongId,
                                    title = songInfo.getString("musicName", ""),
                                    author = radioAuthor,
                                    url = songInfo.getString("songAuditionUrl", ""),
                                    lrc = songInfo.getString("dynamicLyric", ""),
                                    pic = songInfo.getString("largePic", ""),
                                    albumName = songInfo.getString("albumName", "")
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Song(
                                site = "migu",
                                code = 500,
                                msg = e.message ?: "未知异常",
                                songid = songId
                        )
                    }

                }
        return MusicResp.success(data = songs)
    }

    /**
     * 此接口需要完整的地址
     * 根据id获取歌词，部分接口需要传入完整url，由网站确定
     */
    override fun getLrcById(songId: String): String {
        return try {
            val songResp = get(url = songId,
                    timeout = 5.0,
                    headers = mapOf("Referer" to "http://m.10086.cn",
                            "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"))
            if (songResp.statusCode == 200) {
                val songInfo = songResp.text
                songInfo
            } else {
                "[00:00:00]此歌曲可能没有歌词"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("获取歌词出现异常")
            "[00:00:00]此歌曲可能没有歌词"
        }
    }

    private fun JSONObject.getString(key: String, defValue: String = "") = if (this.isNull(key)) {
        defValue
    } else {
        this.getString(key)
    }

}

