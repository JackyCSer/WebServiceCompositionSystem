$(document).ready(function() {
    /** 返回顶部按钮效果 */
    $(window).scroll(function() {
	if ($(window).scrollTop() > 100) {
	    $("#form_top").fadeIn(500);
	} else {
	    $("#form_top").fadeOut(500);
	}
    });
    $("#form_top").click(// 定义返回顶部点击向上滚动的动画
    function() {
	$('body').animate({
	    scrollTop : 0
	}, 200);
    });

    /** 平滑滚动效果 */
    $("#nav1").click(function() {
	$("html,body").animate({
	    scrollTop : $("#section-1").offset().top
	}, 300);
    });
    $("#nav2").click(function() {
	$("html,body").animate({
	    scrollTop : $("#section-2").offset().top
	}, 300);
    });
    $("#nav3-1").click(function() {
	$("html,body").animate({
	    scrollTop : $("#section-3-1").offset().top
	}, 300);
    });
   
});

/** 下拉菜单 */
/* 下拉菜单样式-s2 */
$("#fileTypeSelectUl a").click(function() {
    $("#fileTypeSelect").text($(this).text());
    var name = $(this).attr("name");
    $("fileTypeSelect").attr("name", name);
    fileType = name;
});
/* 下拉菜单样式-s3-1 */
$("#printPlanSelectUl a").click(function() {
    $("#printPlanSelect").text($(this).text());
    var name = $(this).attr("name");
    $("printPlanSelect").attr("name", name);
    printPlan = name;
});

/* 下拉菜单样式-s3-2 */
$("#searchAlgSelectUl a").click(function() {
    $("#searchAlgSelect").text($(this).text());
    var name = $(this).attr("name");
    $("searchAlgSelect").attr("name", name);
    searchAlgorithm = name;
});

/* 下拉菜单样式-s3-3 */
$("#heuristicsSelectUl a").click(function() {
    $("#heuristicsSelect").text($(this).text());
    var name = $(this).attr("name");
    $("heuristicsSelect").attr("name", name);
    // fileType = name;
});

/* 下拉菜单样式-s3-4 */
$("#partialSelectUl a").click(function() {
    $("#partialSelect").text($(this).text());
    var name = $(this).attr("name");
    $("partialSelect").attr("name", name);
    // fileType = name;
});

/** 打印表格函数 */
function printTable(array) {
    var table = [ '<table class="table table-hover table-striped table-bordered amber-font-20">' ];
    // Print t标题
    table.push('<tr>');
    table.push('<th>' + "序号" + '</th>');
    table.push('<th>' + "服务组合结果" + '</th>');
    table.push('</tr>');
    // Print data
    var len = array.length - 1;
    for (var i = 0; i < len; i++) {
	table.push('<tr>');
	table.push('<td>' + (i + 1) + '</td>');
	table.push('<td>' + array[i] + '</td>');
	table.push('</tr>');
    }

    table.push('</table>');
    var table2 = table.join(" ");
    $("#printTable").html(table2);
}