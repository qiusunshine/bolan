package service.model

data class LiveItem(
    var name: String,
    var urls: ArrayList<String>,
    var children: ArrayList<LiveItem>? = null
)