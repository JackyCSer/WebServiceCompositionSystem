//3.1 图搜索过程分析的后两个算法的饼图
var myChart = echarts.init(document.getElementById('main')); // 饼图-1：LAO*X算法
var option = {
    title : {
	text : 'LAO*X 算法效率分析 - 结点利用率',
	x : 'center'
    },
    tooltip : {
	trigger : 'item',
	formatter : "{a} <br/>{b} : {c} ({d}%)"
    },
    legend : {
	orient : 'vertical',
	x : 'left',
	data : [ '利用结点数目', '未利用结点数目' ]
    },
    toolbox : {
	show : true,
	feature : {
	    mark : {
		show : true
	    },
	    dataView : {
		show : true,
		readOnly : false
	    },
	    magicType : {
		show : true,
		type : [ 'pie', 'funnel' ],
		option : {
		    funnel : {
			x : '25%',
			width : '50%',
			funnelAlign : 'left'
		    }
		}
	    },
	    restore : {
		show : true
	    },
	    saveAsImage : {
		show : true
	    }
	}
    },
    calculable : true,
    series : [ {
	name : '结点利用率',
	type : 'pie',
	radius : '55%',
	center : [ '50%', '60%' ],
	data : [ {
	    value : 0,
	    name : '利用结点数目'
	}, {
	    value : 0,
	    name : '未利用结点数目'
	} ]
    } ]
};
myChart.setOption(option);

var myChart1 = echarts.init(document.getElementById('main1')); // 饼图-2：BFS*算法
var option1 = {
    title : {
	text : 'BFS* 算法效率分析 - 结点利用率',
	x : 'center'
    },
    tooltip : {
	trigger : 'item',
	formatter : "{a} <br/>{b} : {c} ({d}%)"
    },
    legend : {
	orient : 'vertical',
	x : 'left',
	data : [ '利用结点数目', '未利用结点数目' ]
    },
    toolbox : {
	show : true,
	feature : {
	    mark : {
		show : true
	    },
	    dataView : {
		show : true,
		readOnly : false
	    },
	    magicType : {
		show : true,
		type : [ 'pie', 'funnel' ],
		option : {
		    funnel : {
			x : '25%',
			width : '50%',
			funnelAlign : 'left'
		    }
		}
	    },
	    restore : {
		show : true
	    },
	    saveAsImage : {
		show : true
	    }
	}
    },
    calculable : true,
    series : [ {
	name : '结点利用率',
	type : 'pie',
	radius : '55%',
	center : [ '50%', '60%' ],
	data : [ {
	    value : 0,
	    name : '利用结点数目'
	}, {
	    value : 0,
	    name : '未利用结点数目'
	} ]
    } ]
};
myChart1.setOption(option1);

// 3.2 组合时间分析 一个 二维时间对比条形图
var myChart2 = echarts.init(document.getElementById('main2'));

var chart2sdata1 = [ 0, 0, 0 ];
var chart2sdata2 = [ 0, 0, 0 ];
var myChart2 = echarts.init(document.getElementById('main2'));
var option2 = {
    color : [ "#0099CC", "#ff7f50" ],
    title : {
	text : '时间对比',
    },
    tooltip : {
	trigger : 'axis'
    },
    legend : {
	data : [ 'LAO*X', 'BFS*' ]
    },
    toolbox : {
	show : true,
	color : '#1e90ff',
	feature : {
	    dataView : {
		show : true,
		readOnly : false
	    },
	    magicType : {
		show : true,
		type : [ 'line', 'bar' ]
	    },
	    saveAsImage : {
		show : true
	    }
	}
    },
    calculable : true,
    xAxis : [ {
	type : 'category',
	data : [ '总时间', '预处理时间', '搜索时间' ]
    } ],
    yAxis : [ {
	name : 'ms',
	type : 'value',
	boundaryGap : [ 0, 0.01 ]
    } ],
    series : [ {
	name : 'LAO*X',
	type : 'bar',
	 itemStyle: {
             normal: {
                 label: {
                     show: true,
                     textStyle: {
                         color: '##000000'
                     }
                 }
             }
         },
	data : chart2sdata1
    }, {
	name : 'BFS*',
	type : 'bar',
	 itemStyle: {
             normal: {
                 label: {
                     show: true,
                     textStyle: {
                         color: '##000000'
                     }
                 }
             }
         },
	data : chart2sdata2
    } ]
};
myChart2.setOption(option2);

window.onresize = function() {
    myChart.resize();
    myChart1.resize();
    myChart2.resize();
};