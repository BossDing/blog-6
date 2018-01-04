var editor = createEditor('editor');
var saveFlag = false;
var loadTemplates = function(){
	return [{name : '基本模板',path:rootPath+'/static/js/mgr/template_simple.html'},{name : '留言模板',path:rootPath+'/static/js/mgr/template_guestbook.html'},
		{name:'首页',path:rootPath+'/static/js/mgr/template_index.html'},{name:'登录页',path:rootPath+'/static/js/mgr/template_login.html'},
		{name:'解锁页',path:rootPath+'/static/js/mgr/template_lock.html'},{name:'文章详情页',path:rootPath+'/static/js/mgr/template_article_detail.html'},
		{name:'文章归档页',path:rootPath+'/static/js/mgr/template_archives.html'}]
}
$(document).ready(function() {
	editor.setSize('100%', $(window).height() - 30);
	sfq.setFileClickFunction(function(path){
		editor.insertUrl(path,true);
		return true;
	});
	
	$('#backupModal').on('show.bs.modal',function(){
		rewriteBaks();
	});
	
		$("#lookupModal").on("show.bs.modal", function() {
			showDataTags();
		});
		$('[data-handler]').click(function(){
			var m = $(this).attr("data-handler");
			switch(m){
			case 'file':
				fileSelectPageQuery(1,'');
	        	$("#fileSelectModal").modal("show");
				break;
			case 'localFile':
				sfq.show();
				break;
			case 'clear':
				bootbox.confirm("确定要清空吗？",function(result){
					if(result){
						editor.clear();
					}
				})
				break;
			case 'format':
				editor.format();
				break;
			case 'lookup':
				lookup();
				break;
			case 'template':
				showTemplateModal();
				break;
			case 'lock':
				showLock();
				break;
			case 'historyTemplate':
				loadHistoryTemplate();
				break;
			default:
				break;
			}
		})
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
			var html = '';
			var id = $(e.target).attr('id');
			switch(id){
			case "data-tab":
				showDataTags();
				break;
			case "fragment-tab":
				showUserFragment(1)
				break;
			}
		});
		
	});
	 $(document).keydown(function(e) {
	    if (e.ctrlKey && e.shiftKey && e.which === 70) {
	        editor.format()
	        return false;
	    }
	    return true;
	 });
	function addDataTag(name){
		editor.replaceSelection('<data name="'+name+'"/>');
		$("#lookupModal").modal('hide');
	}
	function addFragment(name){
		editor.replaceSelection('<fragment name="'+name+'"/>');
		$("#lookupModal").modal('hide')
	}
	
	function showLock(){
		$.get(basePath + '/mgr/lock/all',{},function(data){
			var oldLock = $("#oldLock").val();
			if(data.success){
				var locks = data.data;
				var html = '';
				if(locks.length > 0){
					html += '<div class="table-responsive">';
					html += '<table class="table">';
					for(var i=0;i<locks.length;i++){
						html += '<tr>';
						var lock = locks[i];
						html += '<tr>';
						html += '<td>'+lock.name+'</td>';
						html += '<td><a href="###" onclick="addLock(\''+lock.id+'\')"><span class="glyphicon glyphicon-ok-sign"></span></a></td>';
						html += '</tr>';
					}
					html += '</table>';
					html += '</div>';
					$("#lockBody").html(html);
					$("#lockModal").modal('show')
				} else {
					bootbox.alert("当前没有任何锁");
				}
			}else{
				console.log(data.data);
			}
		});
	}
	
	
	function showTemplateModal(){
		var templates = loadTemplates();
		if(!templates || templates.length == 0){
			bootbox.alert("沒有可供访问的地址");
			return ;
		}
		var html = "<div class='table-responsive'>";
		html += '<table class="table">';
		html += '<tr><td>模板名称</td><td></td></tr>';
		for(var i=0;i<templates.length;i++){
			html += '<tr><td>'+templates[i].name+'</td><td><a href="javascript:void(0)" onclick="loadTemplate(\''+templates[i].path+'\')">加载</a></td></tr>';
		}
		html += '</table>';
		html += '</div>';
		$("#templateModalBody").html(html);
		$("#templateModal").modal('show');
	}
	
	function loadTemplate(path){
		$.get(path,{},function(data){
			editor.setValue(data);
			$("#templateModal").modal('hide');
		})
	}
	
	function addLock(id){
		editor.replaceSelection('<lock id="'+id+'"/>');
		$("#lockModal").modal('hide')
	}
	
	function lookup(){
		$("#lookupModal").modal('show');
	}
	
	function showDataTags(){
		var html = '';
		$('[aria-labelledby="data-tab"]').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/template/dataTags",{},function(data){
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
	
	function showUserFragment(i){
		var html = '';
		$('#fragment').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/template/fragment/list",{"currentPage":i},function(data){
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
			$('#fragment').html(html);
		});
	}
	
	function preview() {
		var page = {"tpl":editor.getValue()};
		var space = $("#spaceSelect").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
		}
		page.name="test";
		page.description="";
		if($.trim($("#alias").val()) != ''){
			page.alias = $.trim($("#alias").val());
		}
		page.allowComment = $("#allowComment").prop("checked");
		$.ajax({
			type : "post",
			url : basePath + '/mgr/template/page/preview',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					var url = data.data;
					if(url.hasPathVariable){
						bootbox.dialog({
							title : '预览地址',
							message : '预览路径为<p><b>'+url.url+'</b></p><p>该地址中包含可变参数，请自行访问</p>',
							buttons : {
								success : {
									label : "确定",
									className : "btn-success"
								}
							}
						});
					} else {
						var win = window.open(url.url,
							'_blank');
						win.focus();
					}
				} else {
					bootbox.alert(data.message);
				}
			},
			complete:function(){
			}
		});
	}
	
	function save() {
		var page = {"tpl":editor.getValue()};
		var space = $("#spaceSelect").val();
		if(space != ''){
			page.space = {"id":space}
		}
		var url = basePath + '/mgr/template/page/create';
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
			url = basePath + '/mgr/template/page/update';
		}
		if($.trim($("#alias").val()) != ''){
			page.alias = $.trim($("#alias").val());
		}
		page.name=$("#name").val();
		page.description=$("#description").val();
		page.allowComment = $("#allowComment").prop("checked");
		saveFlag = true;
		$.ajax({
			type : "post",
			data : JSON.stringify(page),
			url:url,
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					bootbox.alert(data.message);
					if($("#pageKey").val() == "page_"+$("#pageId").val())
						page_storage.removeCurrent();
					setTimeout(function(){
						window.location.href = basePath + '/mgr/template/page/index';
					}, 500);
				} else {
					bootbox.alert(data.message);
					saveFlag = false;
				}
			},
			complete:function(){
			
			}
		});
	}
	
	
	var page_storage = (function() {
		
		var current_tpl;
		
		var getKey = function(){
			var key = $("#pageKey").val();
			if($.trim(key) == ''){
				key = "page_"+$.now();
				$("#pageKey").val(key);
			}
			return key;
		}
		
		setInterval(function(){
			if(saveFlag) return ;
			var content = editor.getValue();
			if($.trim(content) != ''){
				var time = $.now();
				local_storage.store(getKey(),JSON.stringify({"id":getKey(),"content":content,"time":time}));
				$("#auto-save-timer").html("最近备份："+new Date(time).format('yyyy-mm-dd HH:MM:ss'))
			}
			else
				page_storage.removeCurrent();
		},15000);
		
		var v = local_storage.get(getKey());
		if(v != null){
			v = $.parseJSON(v);
			current_tpl = editor.getValue();
			bootbox.confirm("系统发现在"+new Date(v.time).format('yyyy-mm-dd HH:MM:ss')+"留有备份，是否加载？",function(result){
				if(result){
					editor.setValue(v.content);
				}
			})
		}
		
		return {
			listAll:function(){
				var arr = [];
				local_storage.each(function(key,v){
					if(key.indexOf('page_') > -1){
						arr.push({'key':key,"value":v});
					}
				});
				arr.sort(function(x,y){
					var v1 = $.parseJSON(x.value);
					var v2 = $.parseJSON(y.value);
					return  -v1.time + v2.time;
				})
				return arr;
			},
			get:function(key){
				return local_storage.get(key);
			},
			remove:function(key){
				local_storage.remove(key);
			},
			removeCurrent:function(){
				var key = $("#pageKey").val();
				if($.trim(key) != ''){
					local_storage.remove(key);
				}
			}
		}
	}());
	
	function loadBak(key){
		bootbox.confirm("确定要加载吗",function(result){
			if(result){
				var v = page_storage.get(key);
				if(v != null){
					v = $.parseJSON(v);
					$("#pageKey").val(v.id);
					editor.setValue(v.content);
				}
				$("#backupModal").modal('hide');
			}
		})
	}
	
	function rewriteBaks(){
		var baks = page_storage.listAll();
		var html = '<div class="table-responsive">';
		html += '<table class="table">';
		html += '<tr><th>ID</th><th>时间</th><th>操作</th></tr>'
		for(var i=0;i<baks.length;i++){
			var v = $.parseJSON(baks[i].value);
			html += '<tr><td>'+v.id+'</td><td>'+new Date(v.time).format('yyyy-mm-dd HH:MM:ss')+'</td><td><a href="###" onclick="loadBak(\''+v.id+'\')" style="margin-right:5px">加载</a><a href="###" onclick="delBak(\''+v.id+'\')">删除</a></td></tr>';
		}
		html += '</table>';
		html += '</div>';
		$("#backup-body").html(html);
	}
	
	function delBak(key){
		bootbox.confirm("确定要删除吗",function(result){
			if(result){
				page_storage.remove(key);
				rewriteBaks();
			}
		})
	}
	
	function loadHistoryTemplate(){
		var id = $("#pageId").val();
		if(id == ''){
			return ;
		}
		$.get(basePath + '/mgr/template/page/'+id+'/history',{},function(data){
			if(data.success){
				data = data.data;
				if(data.length == 0){
					bootbox.alert("没有历史模板记录");
				} else {
					var html = '<table class="table">';
					html += '<tr><th>备注</th><th>时间</th><th>操作</th></tr>';
					for(var i=0;i<data.length;i++){
						var remark = data[i].remark;
						if(remark.length > 10){
							remark = remark.substring(0,10)+"...";
						}
						html += '<tr><td><a href="###" data-toggle="tooltip" title="'+data[i].remark+'">'+remark+'</a></td><td>'+new Date(data[i].time).format('yyyy-mm-dd HH:MM:ss')+'</td><td><a href="###" data-id="'+data[i].id+'" data-toggle="confirmation"  style="margin-right:10px"><span class="glyphicon glyphicon-ok" aria-hidden="true"></span></a></td></tr>';
					}
					html += '</table>';
					$("#historyTableContainer").html(html);
					$('#historyTableContainer [data-toggle="tooltip"]').tooltip();
					$("#history-tip").html('');
					$("#historyModal").modal('show');
					
					$('[data-toggle=confirmation]').confirmation({
						 rootSelector: '#historyTableContainer',
						 onConfirm:function(){
							 var me = $(this);
							 var id = me.attr('data-id');
							 $.get(basePath + '/mgr/template/history/get/'+id,{},function(data){
								if(data.success){
									editor.setValue(data.data.tpl);
									$("#historyModal").modal('hide')
								}else{
									$("#history-tip").html('<div class="alert alert-warning alert-dismissible"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>'+data.message+'</div>');
								}
							});
						 }
					});
				}
			}else{
				bootbox.alert(data.message);
			}
		});
	}
