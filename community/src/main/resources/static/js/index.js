$(function(){
	$("#publishBtn").click(publish);
});

// 发送AJAX请求之前,将CSRF令牌设置到请求的消息头中.
//    var token = $("meta[name='_csrf']").attr("content");
//    var header = $("meta[name='_csrf_header']").attr("content");
//    $(document).ajaxSend(function(e, xhr, options){
//        xhr.setRequestHeader(header, token);
//    });

function publish() {
	$("#publishModal").modal("hide");

	var title = $("#recipient-name").val();
	var content = $("#message-text").val();

	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title, "content":content},
		function(data)
		{
			data = $.parseJSON(data);
			//在提示框中显示返回信息
			$("#hintBody").text(data.msg);
			//显示提示框
			$("#hintModal").modal("show");
			//两秒后隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				if(data.code == 0)
				{
					window.location.reload();
				}
			}, 2000);
		}
	);


}