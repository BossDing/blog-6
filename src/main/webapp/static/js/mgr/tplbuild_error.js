var editor;
var fragementTplEditor;
var fragements = [];
var _tpls = [];
	$(document).ready(function() {
		editor = editormd("editor", {
			width : "100%",
			height : $(window).height() * 0.8,
			 watch            : false,
             toolbar          : false,
             codeFold         : true,
             searchReplace    : true,
             theme            : "default",
             mode             : "text/html",
             path : basePath + '/static/editor/markdown/lib/'
		});
		fragementTplEditor = editormd("fragementTplEditor", {
			width : "100%",
			height : 600,
			 watch            : false,
             toolbar          : false,
             codeFold         : true,
             searchReplace    : true,
             theme            : "default",
             mode             : "text/html",
             path : basePath + '/static/editor/markdown/lib/'
		});
		$("#loadingModal").on("hidden.bs.modal", function() {
			clearTip();
			$("#loadingModal img").show();
		})
		$("#grabModal").on("hidden.bs.modal", function() {
			$("#loadingModal img").show();
		}).on("show.bs.modal", function() {
			$("#grab_url").val("")
		});
		$("#lookupModal").on("show.bs.modal", function() {
			showDataTags();
		})
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
			var html = '';
			var id = $(e.target).attr('id');
			switch(id){
			case "sys-tab":
				showSysFragement();
				break;
			case "user-tab":
				showUserFragement(1)
				break;
			case "data-tab":
				showDataTags();
				break;
			case "fragement-tab":
				$("#fragementTab").find('li').each(function(){
					if($(this).hasClass('active')){
						switch($(this).find('a').attr('id')){
							case "sys-tab":
								showSysFragement();
								break;
							case "user-tab":
								showUserFragement(1)
								break;
						}
					}
				});
				break;
			}
		})
	});
	
	
	function addDataTag(name){
		editor.insertValue('<data name="'+name+'"/>');
		$("#lookupModal").modal('hide');
	}
	function addFragement(name){
		editor.insertValue('<fragement name="'+name+'"/>');
		$("#lookupModal").modal('hide')
	}
	
	function lookup(){
		$("#lookupModal").modal('show');
	}
	
	function showDataTags(){
		var html = '';
		$('[aria-labelledby="data-tab"]').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/tpl/dataTags",{},function(data){
			if(!data.success){
				bootbox.alert(data.message);
				return ;
			}
			data = data.data;
			html += '<div class=" table-responsive" style="margin-top:10px">';
			html += '<table class="table">';
			for(var i=0;i<data.length;i++){
				html += '<tr>';
				html += '<td>'+data[i]+'</td>';
				html += '<td><a onclick="addDataTag(\''+data[i]+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			$('[aria-labelledby="data-tab"]').html(html);
		});
	}
	
	function showSysFragement(){
		var html = '';
		$('[aria-labelledby="sys-tab"]').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/tpl/sysFragements",{},function(data){
			if(!data.success){
				bootbox.alert(data.message);
				return ;
			}
			data = data.data;
			html += '<div class=" table-responsive" style="margin-top:10px">';
			html += '<table class="table">';
			for(var i=0;i<data.length;i++){
				html += '<tr>';
				html += '<td>'+data[i].name+'</td>';
				html += '<td><a onclick="addFragement(\''+data[i].name+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			$('[aria-labelledby="sys-tab"]').html(html);
		});
	}
	function showUserFragement(i){
		var html = '';
		$('[aria-labelledby="user-tab"]').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/fragement/user/list",{"currentPage":i},function(data){
			if(!data.success){
				bootbox.alert(data.message);
				return ;
			}
			var page = data.data;
			html += '<div class=" table-responsive" style="margin-top:10px">';
			html += '<table class="table">';
			for(var i=0;i<page.datas.length;i++){
				html += '<tr>';
				html += '<td>'+page.datas[i].name+'</td>';
				html += '<td><a onclick="addFragement(\''+page.datas[i].name+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			
			if(page.totalPage > 1){
				html += '<div>';
				html += '<ul class="pagination">';
				for(var i=page.listbegin;i<=page.listend-1;i++){
					html += '<li>';
					html += '<a href="###" onclick="showUserFragement(\''+i+'\')" >'+i+'</a>';
					html += '</li>';
				}
				html += '</ul>';
				html += '</div>';
			}
			$('[aria-labelledby="user-tab"]').html(html);
		});
	}
	
	function preview() {
		var page = {"errorCode":$("#errorCode").val(),"tpl":editor.getValue()};
		page.tpls = fragements;
		var space = $("#space").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
		}
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/error/preview',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					var win = window.open(basePath + '/mgr/page/preview',
							'_blank');
					win.focus();
				} else {
					if(data.message){
						bootbox.alert(data.message);
						return ;
					}
					data = data.data;
					if (data.line) {
						if(data.template && data.template.fragement){
							var fragement = data.template.fragement;
							$("#fragementTplEditModal").modal("show");
							fragementTplEditor.setValue(data.template.tpl);
							if (data.expression) {
								error("第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								error("第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							$("#tpl-save-btn").off("click").on("click",function(){
								var btn  = $(this);
								var data = {tpl:fragementTplEditor.getValue()};
								data.fragement = fragement;
								var exists = false;
								for(var i=0;i<fragements.length;i++){
									var tpl = fragements[i];
									if(tpl.fragement.name == fragement.name){
										tpl.tpl = data.tpl;
										exists = true;
									}
								}
								if(!exists){
									fragements.push(data);
								}
								success("保存成功");
								setTimeout(function(){
									$("#fragementTplEditModal").modal("hide");
									$("#fragementsModal").modal("show");
								},500)
							});
						} else {
							var html = '';
							if (data.templateName) {
								html += '模板:' + data.templateName + ":"
							}
							if (data.expression) {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							if (data.tpl){
								if (editor.mode == 'wysiwyg') {
									editor.setData(fragement.tpl);
									editor.setMode('source')
								} else {
									editor.setMode('wysiwyg', function() {
										editor.setData(fragement.tpl);
										editor.setMode('source');
									});
								}
							}
						}
					}
				}
			},
			complete:function(){
			}
		});
	}
	function save() {
		var page = {"errorCode":$("#errorCode").val(),"tpl":editor.getValue()};
		page.tpls = fragements;
		var space = $("#space").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/error/build',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					bootbox.alert(data.message);
				} else {
					if(data.message){
						bootbox.alert(data.message);
						return ;
					}
					data = data.data;
					if (data.line) {
						if(data.template && data.template.fragement){
							var fragement = data.template.fragement;
							$("#fragementTplEditModal").modal("show");
							fragementTplEditor.setValue(data.template.tpl);
							if (data.expression) {
								error("第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								error("第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							$("#tpl-save-btn").off("click").on("click",function(){
								var btn  = $(this);
								var data = {tpl:fragementTplEditor.getValue()};
								data.fragement = fragement;
								var exists = false;
								for(var i=0;i<fragements.length;i++){
									var tpl = fragements[i];
									if(tpl.fragement.name == fragement.name){
										tpl.tpl = data.tpl;
										exists = true;
									}
								}
								if(!exists){
									fragements.push(data);
								}
								success("保存成功");
								setTimeout(function(){
									$("#fragementTplEditModal").modal("hide");
									$("#fragementsModal").modal("show");
								},500)
							});
						} else {
							var html = '';
							if (data.templateName) {
								html += '模板:' + data.templateName + ":"
							}
							if (data.expression) {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							if (data.tpl){
								if (editor.mode == 'wysiwyg') {
									editor.setData(fragement.tpl);
									editor.setMode('source')
								} else {
									editor.setMode('wysiwyg', function() {
										editor.setData(fragement.tpl);
										editor.setMode('source');
									});
								}
							}
						}
					}
				}
			},
			complete:function(){
			}
		});
	}
	function grab() {
		$("#grabModal").modal('hide');
		$("#loadingModal").modal({
			backdrop : 'static',
			keyboard : false
		});
		$.get(basePath + "/mgr/page/grab", {
			"url" : $("#grab_url").val()
		}, function callBack(data) {
			if (data.success) {
				var value = data.data;
				if (editor.mode == 'wysiwyg') {
					editor.setData(value);
				} else {
					editor.setMode('wysiwyg', function() {
						editor.setData(value);
					});
				}
				$("#loadingModal").modal("hide")
			} else {
				$("#loadingModal img").hide();
				error(data.message);
				setTimeout(function() {
					$("#loadingModal").modal("hide")
				}, 2000)
			}
		})
	}
	function revert(pageId,fragementId,fragementType){
		$("#fragementsModal").modal('hide');
		$.ajax({
			type : "post",
			 async: false,
			url : basePath+"/mgr/page/ERROR/"+pageId+"/fragement/"+fragementType+"/"+fragementId+"/delete",
			data : {},
			success : function(data){
				if(data.success){
					success(data.message);
					setTimeout(function(){
						$("#fragementsModal").modal('show');
					},500);
					flag = true;
				} else {
					error(data.message);
				}
			},
			complete:function(){
			}
		});
	}