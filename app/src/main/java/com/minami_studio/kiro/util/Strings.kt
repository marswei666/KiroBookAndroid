package com.minami_studio.kiro.util

/**
 * 内联多语言字符串（与 iOS Strings struct 完全一致）
 * 所有 UI 文本通过 pick() 函数按当前语言返回对应翻译
 */
class Strings(val lang: AppLanguage) {

    private fun pick(zh: String, en: String, ja: String, ko: String, zht: String): String =
        when (lang) {
            AppLanguage.simplifiedChinese -> zh
            AppLanguage.english -> en
            AppLanguage.japanese -> ja
            AppLanguage.korean -> ko
            AppLanguage.traditionalChinese -> zht
        }

    // ── Common ──
    val cancel get() = pick("取消", "Cancel", "キャンセル", "취소", "取消")
    val save get() = pick("保存", "Save", "保存", "저장", "儲存")
    val close get() = pick("关闭", "Close", "閉じる", "닫기", "關閉")
    val delete get() = pick("删除", "Delete", "削除", "삭제", "刪除")
    val edit get() = pick("编辑", "Edit", "編集", "편집", "編輯")
    val all get() = pick("全部", "All", "すべて", "전체", "全部")
    val add get() = pick("添加", "Add", "追加", "추가", "新增")
    val ok get() = pick("好", "OK", "OK", "확인", "好")
    val city get() = pick("城市", "City", "都市", "도시", "城市")
    val country get() = pick("国家", "Country", "国", "국가", "國家")
    val cities get() = pick("城市", "Cities", "都市", "도시", "城市")
    val countries get() = pick("国家", "Countries", "国", "국가", "國家")
    val confirm get() = pick("确认", "Confirm", "確認", "확인", "確認")
    val name get() = pick("名称", "Name", "名前", "이름", "名稱")

    // ── Tab Bar ──
    val tabHome get() = pick("首页", "Home", "ホーム", "홈", "首頁")
    val tabMap get() = pick("地图", "Map", "マップ", "지도", "地圖")
    val tabCollection get() = pick("收藏", "Collections", "コレクション", "컬렉션", "收藏")
    val tabProfile get() = pick("我的", "Profile", "プロフィール", "프로필", "我的")

    // ── Home ──
    val homeCheckIns get() = pick("打卡", "Check-ins", "チェックイン", "체크인", "打卡")
    val homeNoEntries get() = pick("还没有打卡记录", "No entries yet", "まだ記録なし", "기록이 없습니다", "尚無打卡記錄")
    val homeNoEntriesHint get() = pick("点击下方 + 开始记录你的第一个探店", "Tap + below to log your first spot", "下の + をタップして最初のスポットを記録", "아래 + 버튼으로 첫 기록을 시작해보세요", "點擊下方 + 開始記錄第一個探店")

    // ── Add Entry ──
    val newEntry get() = pick("新建打卡", "New Entry", "新規記録", "새 기록", "新建打卡")
    val editEntry get() = pick("编辑打卡", "Edit Entry", "記録を編集", "기록 편집", "編輯打卡")
    val photos get() = pick("照片", "Photos", "写真", "사진", "照片")
    val category get() = pick("类型", "Category", "カテゴリ", "카테고리", "類型")
    val location get() = pick("位置", "Location", "場所", "위치", "位置")
    val addressPlaceholder get() = pick("粘贴Google地址，搜索定位", "Paste Google address to search", "Google住所を貼り付けて検索", "Google 주소 붙여넣고 검색", "貼上Google地址，搜尋定位")
    val addressNotFound get() = pick("未找到该地址，请检查输入", "Address not found, please check your input", "住所が見つかりません、入力内容を確認してください", "주소를 찾을 수 없습니다, 입력을 확인하세요", "未找到該地址，請檢查輸入")
    val locating get() = pick("定位中...", "Locating...", "位置取得中...", "위치 확인 중...", "定位中...")
    val autoLocate get() = pick("自动定位", "Auto-locate", "自動定位", "자동 위치", "自動定位")
    val coordinateObtained get() = pick("已获取坐标，将显示在地图上", "Coordinates obtained, will show on map", "座標取得済み、地図に表示されます", "좌표 확인, 지도에 표시됩니다", "已獲取座標，將顯示在地圖上")
    val shopNamePlaceholder get() = pick("店名", "Shop name", "店舗名", "상호명", "店名")
    val visitDate get() = pick("探访日期", "Visit Date", "訪問日", "방문 날짜", "探訪日期")
    val rating get() = pick("评分", "Rating", "評価", "평점", "評分")
    val myNotes get() = pick("我的感受", "My Notes", "メモ", "메모", "我的感受")
    val notesPlaceholder get() = pick("写下你的感受，只给自己看...", "Write your thoughts, just for you...", "あなたの気持ちを書いてください...", "나만의 기록을 남겨보세요...", "寫下你的感受，只給自己看...")
    val saveFailed get() = pick("保存失败", "Save Failed", "保存失敗", "저장 실패", "儲存失敗")
    val noCoordTitle get() = pick("未获取经纬度", "No Coordinates", "座標未取得", "좌표 없음", "未獲取經緯度")
    val noCoordMessage get() = pick("请通过搜索定位或自动定位获取经纬度。", "Please use address search or auto-locate to get coordinates.", "住所検索または自動定位で座標を取得してください。", "주소 검색 또는 자동 위치로 좌표를 가져오세요.", "請透過搜尋定位或自動定位獲取經緯度。")
    val goBack get() = pick("返回", "Go Back", "戻る", "돌아가기", "返回")

    // ── Entry Detail ──
    val deleteEntryTitle get() = pick("删除这条打卡？", "Delete this entry?", "この記録を削除しますか？", "이 기록을 삭제하시겠습니까?", "刪除這條打卡？")
    val deleteEntryMessage get() = pick("此操作无法撤销，照片也会一并删除。", "This action cannot be undone. Photos will also be deleted.", "この操作は取り消せません。写真も削除されます。", "이 작업은 취소할 수 없습니다. 사진도 함께 삭제됩니다.", "此操作無法撤銷，照片也會一並刪除。")
    val myNotesLabel get() = pick("我的感受", "My Notes", "メモ", "메모", "我的感受")
    val mood get() = pick("心情", "Mood", "気分", "기분", "心情")
    val fullscreenView get() = pick("全屏查看", "Full Screen", "全画面", "전체 화면", "全螢幕")

    // ── Collection ──
    val collectionTitle get() = pick("收藏", "Collections", "コレクション", "컬렉션", "收藏")
    val byCategory get() = pick("品类", "Category", "カテゴリ", "카테고리", "品類")
    val byCountry get() = pick("国家", "Country", "国", "국가", "國家")
    val favorites get() = pick("收藏", "Favorites", "お気に入り", "즐겨찾기", "收藏")
    val emptyCountryHint get() = pick("打卡时填写城市/国家，就能在这里看到", "Fill in city/country when logging to see them here", "記録時に都市・国を入力するとここに表示されます", "기록할 때 도시/국가를 입력하면 여기에 표시됩니다", "打卡時填寫城市/國家，就能在這裡看到")
    val emptyFavoritesHint get() = pick("在打卡详情页点击书签，收藏你最爱的地方", "Bookmark entries to save your favorites", "詳細画面でブックマークしてお気に入りを保存", "상세 화면에서 북마크를 탭해 즐겨찾기 저장", "在打卡詳情頁點擊書籤，收藏你最愛的地方")
    fun seeAll(count: Int) = pick("查看全部 $count 条", "See all $count", "すべて見る ($count)", "전체 보기 ($count)", "查看全部 $count 條")
    fun entriesCount(count: Int) = pick("$count 个打卡", "$count entries", "$count 件", "$count 개", "$count 個打卡")

    // ── Map ──
    val mapTitle get() = pick("地图", "Map", "マップ", "지도", "地圖")
    val noMapEntries get() = pick("暂无地图打卡", "No map entries", "地図の記録なし", "지도 기록 없음", "暫無地圖打卡")
    val noMapEntriesHint get() = pick("打卡时开启定位，记录就会出现在地图上", "Enable location when logging to show on map", "記録時に位置情報をオンにすると地図に表示されます", "기록 시 위치를 활성화하면 지도에 표시됩니다", "打卡時開啟定位，記錄就會出現在地圖上")

    // ── Profile ──
    val profileTitle get() = pick("我的手账", "My Journal", "マイ手帳", "나의 여행 노트", "我的手帳")
    val profileTagline get() = pick("记录每一个值得被记住的角落", "Capture every corner worth remembering", "記憶に残る場所を記録しよう", "기억할 가치 있는 모든 공간을 기록하세요", "記錄每一個值得被記住的角落")
    val totalCheckIns get() = pick("打卡总数", "Total", "合計", "전체", "打卡總數")
    val categoryBreakdown get() = pick("品类分布", "By Category", "カテゴリ別", "카테고리별", "品類分佈")
    fun visitedCountries(count: Int) = pick("去过的国家 · $count", "Countries · $count", "訪問国 · $count", "방문 국가 · $count", "去過的國家 · $count")
    val storage get() = pick("存储", "Storage", "ストレージ", "저장소", "儲存")
    val photoStorage get() = pick("照片占用空间", "Photo Storage", "写真のストレージ", "사진 저장소", "照片佔用空間")
    val privacyNote get() = pick("所有数据仅保存在本设备，不上传任何服务器", "All data is stored on this device only", "すべてのデータはデバイスにのみ保存されます", "모든 데이터는 이 기기에만 저장됩니다", "所有數據僅保存在本設備，不上傳任何伺服器")
    val exportBackup get() = pick("导出备份", "Export Backup", "バックアップを書き出す", "백업 내보내기", "匯出備份")
    val importBackup get() = pick("导入备份", "Import Backup", "バックアップを読み込む", "백업 가져오기", "匯入備份")
    val aboutWander get() = pick("关于 Kiro Book", "About Kiro Book", "Kiro Bookについて", "Kiro Book 정보", "關於 Kiro Book")

    // ── Export ──
    val exportTitle get() = pick("备份你的手账", "Backup Your Journal", "手帳をバックアップ", "여행 노트 백업", "備份你的手帳")
    val exportDesc get() = pick("导出 .zip 备份文件，包含所有打卡记录和照片\n可通过文件 App 迁移到新设备", "Export a .zip backup with all entries and photos\nTransfer via Files app to a new device", "すべての記録と写真を含む.zipバックアップを書き出します\nファイルAppで新しいデバイスに転送", "모든 기록과 사진이 담긴 .zip 백업을 내보냅니다\n파일 앱으로 새 기기에 전송", "匯出 .zip 備份檔案，包含所有打卡記錄和照片\n可透過檔案 App 遷移到新裝置")
    fun exportEntriesCount(count: Int) = pick("$count 条打卡记录", "$count entries", "$count 件の記録", "$count 개의 기록", "$count 條打卡記錄")
    fun exportPhotoSize(size: String) = pick("照片占用 $size", "Photos: $size", "写真: $size", "사진: $size", "照片佔用 $size")
    val exportButton get() = pick("导出备份", "Export", "書き出す", "내보내기", "匯出備份")

    // ── Import ──
    val importTitle get() = pick("还原你的手账", "Restore Your Journal", "手帳を復元", "여행 노트 복원", "還原你的手帳")
    val importDesc get() = pick("选择之前导出的 .zip 备份文件\n记录和照片都会一并还原，已有记录不会重复导入", "Select a previously exported .zip backup\nEntries and photos will be restored. Existing entries won't be duplicated", "以前に書き出した.zipバックアップを選択\n記録と写真が復元されます。既存の記録は重複しません", "이전에 내보낸 .zip 백업 파일 선택\n기록과 사진이 복원됩니다. 기존 기록은 중복되지 않습니다", "選擇之前匯出的 .zip 備份檔案\n記錄和照片都會一併還原，已有記錄不會重複匯入")
    val importButton get() = pick("导入备份", "Import", "読み込む", "가져오기", "匯入備份")
    val importErrCannotRead get() = pick("无法读取文件，请重试", "Cannot read file, please try again", "ファイルを読み取れません、もう一度お試しください", "파일을 읽을 수 없습니다. 다시 시도해주세요", "無法讀取檔案，請重試")
    val importErrReadFailed get() = pick("文件读取失败", "File read failed", "ファイル読み取り失敗", "파일 읽기 실패", "檔案讀取失敗")
    val importErrInvalidFormat get() = pick("格式不正确，请选择 WanderLog 导出的备份文件", "Invalid format, please select a WanderLog backup", "形式が正しくありません。WanderLogのバックアップを選択してください", "올바른 형식이 아닙니다. WanderLog 백업 파일을 선택해주세요", "格式不正確，請選擇 WanderLog 匯出的備份檔案")
    val importNoNew get() = pick("没有新记录可导入", "No new entries to import", "新しい記録はありません", "가져올 새 기록이 없습니다", "沒有新記錄可匯入")
    fun importSuccess(count: Int) = pick("成功导入 $count 条记录", "Successfully imported $count entries", "$count 件の記録を読み込みました", "$count 개의 기록을 가져왔습니다", "成功匯入 $count 條記錄")

    // ── About ──
    val appSubtitle get() = pick("全球探店电子手账", "Global Shop Diary", "グローバル探店ダイアリー", "글로벌 탐방 다이어리", "全球探店電子手帳")
    fun version(v: String) = pick("版本 $v", "Version $v", "バージョン $v", "버전 $v", "版本 $v")
    val aboutPrivacy1 get() = pick("所有数据仅保存在你的设备", "All data stays on your device", "すべてのデータはデバイスに保存", "모든 데이터는 기기에 저장", "所有數據僅保存在你的裝置")
    val aboutPrivacy2 get() = pick("完全离线可用", "Works fully offline", "完全オフライン対応", "완전 오프라인 지원", "完全離線可用")
    val aboutPrivacy3 get() = pick("无账号，无追踪，无广告", "No account, no tracking, no ads", "アカウント不要、追跡なし、広告なし", "계정 없음, 추적 없음, 광고 없음", "無帳號，無追蹤，無廣告")
    val about get() = pick("关于", "About", "について", "정보", "關於")
    val traveler get() = pick("旅行者", "Traveler", "旅人", "여행자", "旅行者")
    val selectFile get() = pick("选择文件", "Select File", "ファイルを選択", "파일 선택", "選擇檔案")
    val shareBackup get() = pick("分享备份", "Share Backup", "バックアップを共有", "백업 공유", "分享備份")
    val saveToLocal get() = pick("保存到本地", "Save to Device", "デバイスに保存", "기기에 저장", "儲存到本機")

    // ── Icon Picker / Category Editor ──
    val addCategory get() = pick("新增类型", "Add Category", "カテゴリを追加", "카테고리 추가", "新增類型")
    val editCategory get() = pick("编辑类型", "Edit Category", "カテゴリを編集", "카테고리 편집", "編輯類型")
    val categoryNameLabel get() = pick("类型名称", "Category Name", "カテゴリ名", "카테고리 이름", "類型名稱")
    val categoryNamePlaceholder get() = pick("输入类型名称", "Enter category name", "カテゴリ名を入力", "카테고리 이름 입력", "輸入類型名稱")
    val preview get() = pick("预览", "Preview", "プレビュー", "미리보기", "預覽")
    val selectIcon get() = pick("选择图标", "Select Icon", "アイコンを選択", "아이콘 선택", "選擇圖示")
    val dragToSort get() = pick("长按拖拽可排序", "Hold to reorder", "長押しで並べ替え", "길게 눌러 정렬", "長按拖曳可排序")

    // ── Icon Group Names ──
    val iconGroupFood get() = pick("餐饮", "Food & Drink", "飲食", "식음료", "餐飲")
    val iconGroupCulture get() = pick("文化", "Culture", "文化", "문화", "文化")
    val iconGroupShopping get() = pick("购物", "Shopping", "ショッピング", "쇼핑", "購物")
    val iconGroupLeisure get() = pick("休闲", "Leisure", "レジャー", "여가", "休閒")
    val iconGroupPlaces get() = pick("场所", "Places", "場所", "장소", "場所")
    val iconGroupOther get() = pick("其他", "Other", "その他", "기타", "其他")

    // ── Calendar ──
    val weekdayAbbreviations: List<String>
        get() = when (lang) {
            AppLanguage.simplifiedChinese, AppLanguage.traditionalChinese ->
                listOf("日", "一", "二", "三", "四", "五", "六")
            AppLanguage.english ->
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
            AppLanguage.japanese ->
                listOf("日", "月", "火", "水", "木", "金", "土")
            AppLanguage.korean ->
                listOf("일", "월", "화", "수", "목", "금", "토")
        }

    fun monthYear(year: Int, month: Int) = pick(
        "${year}年${month}月",
        "$year / $month",
        "${year}年${month}月",
        "${year}년 ${month}월",
        "${year}年${month}月"
    )

    // ── Misc ──
    val shopName get() = pick("店名", "Shop Name", "店舗名", "상호명", "店名")
    val coordinates get() = pick("坐标", "Coordinates", "座標", "좌표", "座標")
    val latitude get() = pick("纬度", "Latitude", "緯度", "위도", "緯度")
    val longitude get() = pick("经度", "Longitude", "経度", "경도", "經度")
    val fullMap get() = pick("全屏地图", "Full Map", "全画面マップ", "전체 지도", "全螢幕地圖")
}
