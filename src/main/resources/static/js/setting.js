$(function(){
    $("#uploadForm").submit(upload); //upload是 下文的函数
});

function upload() {
    $.ajax({           //$.get、$.post是该方法的简化
        url: "http://upload-z2.qiniup.com",
        method: "post",
        processData: false,   //不把表单内容转换为字符串
        contentType: false,  //不让jquery设置上传类型
        data: new FormData($("#uploadForm")[0]),   //$("#uploadForm")[0] ———— js对象
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径；还是异步请求
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();//更新头像成功，刷新页面
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false;  //return false;  代表表单提交到此结束，不再向下执行。前面把上传头像的逻辑给做完了
}