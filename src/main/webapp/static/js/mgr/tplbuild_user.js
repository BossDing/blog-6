var editor;
var fragmentTplEditor;
var fragments = [];
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
		fragmentTplEditor = editormd("fragmentTplEditor", {
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
			clearTip();
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
				showSysFragment();
				break;
			case "user-tab":
				showUserFragment(1)
				break;
			case "data-tab":
				showDataTags();
				break;
			case "fragment-tab":
				$("#fragmentTab").find('li').each(function(){
					if($(this).hasClass('active')){
						switch($(this).find('a').attr('id')){
							case "sys-tab":
								showSysFragment();
								break;
							case "user-tab":
								showUserFragment(1)
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
	function addFragment(name){
		editor.insertValue('<fragment name="'+name+'"/>');
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
	
	function showSysFragment(){
		var html = '';
		$('[aria-labelledby="sys-tab"]').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/tpl/sysFragments",{},function(data){
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
				html += '<td><a onclick="addFragment(\''+data[i].name+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			$('[aria-labelledby="sys-tab"]').html(html);
		});
	}
	function showUserFragment(i){
		var html = '';
		$('[aria-labelledby="user-tab"]').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/fragment/user/list",{"currentPage":i},function(data){
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
				html += '<td><a onclick="addFragment(\''+page.datas[i].name+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			
			if(page.totalPage > 1){
				html += '<div>';
				html += '<ul class="pagination">';
				for(var i=page.listbegin;i<=page.listend-1;i++){
					html += '<li>';
					html += '<a href="###" onclick="showUserFragment(\''+i+'\')" >'+i+'</a>';
					html += '</li>';
				}
				html += '</ul>';
				html += '</div>';
			}
			$('[aria-labelledby="user-tab"]').html(html);
		});
	}
	function preview() {
		var page = {"tpl":editor.getValue()};
		page.tpls = fragments;
		var space = $("#space").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
		}
		page.name="test";
		page.description="";
		page.alias = "test";
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/user/preview',
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
						if(data.template && data.template.fragment){
							var fragment = data.template.fragment;
							$("#fragmentTplEditModal").modal("show");
							fragmentTplEditor.setValue(data.template.tpl);
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
								var data = {tpl:fragmentTplEditor.getValue()};
								data.fragment = fragment;
								var exists = false;
								for(var i=0;i<fragments.length;i++){
									var tpl = fragments[i];
									if(tpl.fragment.name == fragment.name){
										tpl.tpl = data.tpl;
										exists = true;
									}
								}
								if(!exists){
									fragments.push(data);
								}
								success("保存成功");
								setTimeout(function(){
									$("#fragmentTplEditModal").modal("hide");
									$("#fragmentsModal").modal("show");
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
								editor.setValue(data.tpl)
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
		var page = {"target":$("#target").val(),"tpl":editor.getValue()};
		page.tpls = fragments;
		var space = $("#spaceSelect").val();
		if(space != ''){
			page.space = {"id":space}
		}
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
		}
		if($.trim($("#alias").val()) != ''){
			page.alias = $.trim($("#alias").val());
		}
		page.name=$("#name").val();
		page.description=$("#description").val();
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/user/build',
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
						if(data.template && data.template.fragment){
							var fragment = data.template.fragment;
							$("#fragmentTplEditModal").modal("show");
							$("#fragmentTplEditor").setValue(data.template.tpl);
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
								var data = {tpl:fragmentTplEditor.getValue()};
								data.fragment = fragment;
								var exists = false;
								for(var i=0;i<fragments.length;i++){
									var tpl = fragments[i];
									if(tpl.fragment.name == fragment.name){
										tpl.tpl = data.tpl;
										exists = true;
									}
								}
								if(!exists){
									fragments.push(data);
								}
								success("保存成功");
								setTimeout(function(){
									$("#fragmentTplEditModal").modal("hide");
									$("#fragmentsModal").modal("show");
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
								editor.setValue(data.tpl)
							}
						}
					}
				}
			},
			complete:function(){
			}
		});
	}
	
	function revert(pageId,fragmentId,fragmentType){
		$("#fragmentsModal").modal('hide');
		$.ajax({
			type : "post",
			 async: false,
			url : basePath+"/mgr/page/USER/"+pageId+"/fragment/"+fragmentType+"/"+fragmentId+"/delete",
			data : {},
			success : function(data){
				if(data.success){
					success(data.message);
					setTimeout(function(){
						$("#fragmentsModal").modal('show');
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