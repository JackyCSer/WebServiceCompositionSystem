<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<title>Luna Planner</title>

<link href="./css/bootstrap.css" rel="stylesheet" />
<link href="./css/bootstrap-theme.css" rel="stylesheet" />
<link href="./css/jacky-demo.css" rel="stylesheet" />

<style type="text/css">
p.amber-p-center {
	text-align: center;
}

p.amber-p-font {
	font-size: 20px;
}
</style>
<!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
<!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
</head>
<body>

	<!--↓↓↓---导航---↓↓↓-->
	<div class="navbar navbar-fixed-top">
		<div class="container">
			<a class="brand" href="#">Luna Planner</a>
		</div>
		<!-- /container -->
	</div>
	<!-- /navbar -->
	<!--↑↑↑---导航---↑↑↑-->

	<!--↓↓↓---内容---↓↓↓-->
	<div id="content">
		<div class="container">
			<div class="row">
				<div class="col-lg-3">
					<!--↓输入-->
					<div class="alert alert-success" role="alert">
						<p class="amber-p-center2 amber-p-font">
							<strong>INPUT</strong>
						</p>
					</div>

					<div class="panel panel-default">
						<!--输入面板-->
						<div class="panel-heading">
							<strong>Input PDDL Files</strong>
						</div>

						<form role="form" action="upload.action" method="post"
							enctype="multipart/form-data">
							<table class="table table-striped">
								<tr>
									<th><strong>Domain PDDL File</strong></th>
								</tr>
								<tr>
									<td><input type='text' name='textfield1' id='textfield1'
										class='form-control' /> <input type="button"
										class="btn btn-default" value="Browse..." size="30"
										onclick="f1.click()" /> <input type="file" id="f1"
										onchange="document.getElementById('textfield1').value=this.value"
										name="domain"
										style="position:absolute; filter:alpha(opacity=0); opacity:0; width:0px; "
										size="1" /></td>
								</tr>

								<tr>
									<th><strong>Problem PDDL File</strong></th>
								</tr>
								<tr>
									<td><input type='text' name='textfield2' id='textfield2'
										class='form-control' /> <input type="button"
										class="btn btn-default" value="Browse..." size="30"
										onclick="f2.click()" /> <input type="file" id="f2"
										onchange="document.getElementById('textfield2').value=this.value"
										name="problem"
										style="position:absolute; filter:alpha(opacity=0); opacity:0; width:0px; "
										size="1" /> <br /> <br />
										<p class="amber-p-center">
											<button class="btn btn-primary btn-large" type="submit"
												id="submit-btn">Submit</button>
										</p>
										<div id="load"></div></td>
								</tr>
							</table>
						</form>
					</div>
					<!--输入面板-->

					<div class="panel panel-default">
						<!--输入面板-->
						<div class="panel-heading">
						<strong>Choose PDDL File Type</strong>
						</div>
						<div class="panel-body">

							<div class="btn-group">
								<button type="button" class="btn btn-info" id="fileTypeSelect"
									name="FOND">FOND</button>
								<button type="button" class="btn btn-info dropdown-toggle"
									data-toggle="dropdown">
									<span class="caret"></span>
								</button>
								<ul class="dropdown-menu" role="menu" id="fileTypeSelectUl">
									<li><a name="FOND">FOND</a></li>
									<li><a name="POND">POND</a></li>
								</ul>
							</div>
					
							<p class="amber-p-center">
								<input type="button" class="btn btn-primary btn-large"
									id="translate-btn" value="Translate" />
							</p>
							<p class="amber-p-center">
								<input type="button" class="btn btn-primary btn-large"
									id="execute-btn" value="Execute" />
							</p>

						</div>
					</div>
					<!--输入面板-->
					
							<div class="panel panel-default">
						<!--输入面板: Print Plan-->
						<div class="panel-heading">
						<strong>Print the Plan</strong>
						</div>
						<div class="panel-body">

							<div class="btn-group">
								<button type="button" class="btn btn-info" id="printPlanSelect"
									name="yes">YES</button>
								<button type="button" class="btn btn-info dropdown-toggle"
									data-toggle="dropdown">
									<span class="caret"></span>
								</button>
								<ul class="dropdown-menu" role="menu" id="printPlanSelectUl">
									<li><a name="yes">YES</a></li>
									<li><a name="no">NO</a></li>
								</ul>
							</div>
					
							<p class="amber-p-center">
								<input type="button" class="btn btn-primary btn-large"
									id="submit-btn" value="Submit" />
							</p>

						</div>
					</div>
					<!--输入面板-->


				</div>
				<!--↑输入-->

				<div class="col-lg-9">
					<!--↓执行-->
					<div class="alert alert-danger" role="alert">
						<p class="amber-p-center2 amber-p-font">
							<strong>OUTPUT</strong>
						</p>
					</div>

					<div class="panel panel-default">
						<!-- Default panel contents -->
						<div class="panel-heading">

						</div>
						<div class="panel-body">
							<h2>Translator Output</h2>
							<textarea id="output1" class="form-control" rows="20"  style="font-size:20px;"></textarea>
							<h2>Planner Output</h2>
							<textarea id="output2" class="form-control" rows="20"  style="font-size:20px;"></textarea>
							<h2>SAS File</h2>
							<div class="embed-responsive embed-responsive-4by3">
								<iframe class="embed-responsive-item"
									src="./translator_output/output.sas" ></iframe>
							</div>

						</div>
					</div>

				</div>
				<!--↑执行-->

				<!--↑输出-->
			</div>
		</div>
		<!-- /container -->
	</div>
	<!-- /content -->

	<div id="footer">
		<div class="container">
			<hr />
			<p class="amber-p-center">Jacky&copy;2015 - 毕业设计</p>
		</div>
		<!-- /container -->
	</div>
	<!-- /footer -->





	<!-- Le javascript
================================================== -->
	<!-- Placed at the end of the document so the pages load faster -->
	<script src="./js/jquery-2.1.3.min.js"></script>
	<script src="./js/bootstrap.min.js"></script>
	<script src="./js/esl.js"></script>
	<script type="text/javascript">
		//Action请求的全局URL
		var url1 = "luna/translate.action";
		var fileType = "FOND";
		var url2 = "luna/execute.action";

		//load方法测试data
		//$("#load").load("dealfile.action",{ file1FileName: "${file1name}", file2FileName: "${file2name}" });

		/*下拉菜单样式*/
		$("#fileTypeSelectUl a").click(function()
		{
			$("#fileTypeSelect").text($(this).text());
			var name = $(this).attr("name");
			$("fileTypeSelect").attr("name", name);
			fileType = name;
		});

		$("#translate-btn").click(function()
		{
			/* 			$("#output1").load(url1,{
			 parameters:"{fileType :"+fileType+"}"
			 }); */

			//发送请求
			$.getJSON(url1, {
				parameters : "{fileType :" + fileType + "}"
			}, function(data)
			{
				var str = data.translatorOutput;
				$("#output1").html(str);
			});

		});
		$("#execute-btn").click(function()
		{
			/* 		$("#output2").load(url2); */

			//发送请求
			$.getJSON(url2, function(data)
			{
				var str = data.plannerOutput;
				$("#output2").html(str);
			});

		});
	</script>

</body>
</html>
