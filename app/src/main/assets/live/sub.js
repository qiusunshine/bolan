function loadSub(reload){
    $.get("/api/sub/get",function(data, status){
        $("#url").val(data || '');
        if(reload) {
            setTimeout(() => {
                loadSub(false);
            }, 1500);
        }
     });
}
window.loadSub = loadSub;

function showInfo(text){
    $("#modal-info-content").text(text);
    $("#modal-info").modal('show');
}
$("#info-modal-btn").click(function(){
    $("#modal-info").modal('hide');
});

$("#save").click(function(){
    let u = $("#url").val();
    if(u == null || u == "" || u.indexOf("http") != 0){
        showInfo('地址有误');
        return;
    }
    $.ajax({
        type: "post",
        url: '/api/sub/set',
        async: false,
        data: JSON.stringify({url:u}),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        complete: function(data) {
            showInfo('订阅已保存');
            window.loadSub(false);
        }
    });
});

window.loadSub(false);