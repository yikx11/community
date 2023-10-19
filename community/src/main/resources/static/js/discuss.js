$(function ()//在页面加载完后就会调用，类似于window.onload
{
    $("#topBtn").click(setTop);//js获取id的时候。。必须加 # 啊啊啊！！
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});
function like(btn, entityType, entityId, entityUserId, postId)
{
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        function (data)
        {
            data = $.parseJSON(data);
            if(data.code == 0)
            {
                $(btn).children("b").text(data.likeStatus == 1 ? '已赞':'赞');
                $(btn).children("i").text(data.likeCount);
            }
            else
            {
                alert(data.msg);
            }
        }
    );


}

//置顶
function setTop()
{
    $.post(
        CONTEXT_PATH + "/discuss/top",
        {"id":$("#postId").val()},
        function (data){
            data = $.parseJSON(data);
            if(data.code == 0)
            {
                $("#topBtn").attr("disabled", "disabled");
            }
            else {
                alert(data.msg);
            }
        }
    );
}

// 加精
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $("#wonderfulBtn").attr("disabled", "disabled");
            } else {
                alert(data.msg);
            }
        }
    );
}

// 删除
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        }
    );
}