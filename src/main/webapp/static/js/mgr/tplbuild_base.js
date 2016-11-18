var editor;
var fragments = [];
var _tpls = [];
	$(document).ready(function() {
		var mixedMode = {
		        name: "htmlmixed",
		        scriptTypes: [{matches: /\/x-handlebars-template|\/x-mustache/i,
		                       mode: null},
		                      {matches: /(text|application)\/(x-)?vb(a|script)/i,
		                       mode: "vbscript"}]
		      };
	    editor = CodeMirror.fromTextArea(document.getElementById("editor"), {
	        mode: mixedMode,
	        lineNumbers: true,
	        autoCloseTags: true,
	        extraKeys: {"Alt-/": "autocomplete"}
	      });
	    editor.setSize('100%',$(window).height() * 0.8);
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
		editor.replaceSelection('<data name="'+name+'"/>');
		$("#lookupModal").modal('hide');
	}
	function addFragment(name){
		editor.replaceSelection('<fragment name="'+name+'"/>');
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
	function showError(data){
		if(data.message){
			bootbox.alert(data.message);
			return ;
		}
		data = data.data;
		if (data.line) {
			if(data.template && data.template.fragment){
				var fragment = data.template.fragment;
				if (data.expression) {
					bootbox.alert(fragment+"第" + data.line + "行,"
							+ data.col + "列，表达式：" + data.expression
							+ "发生错误")
				} else {
					bootbox.alert(fragment+"第" + data.line + "行,"
							+ data.col + "列发生错误")
				}
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
					editor.setValue(fragment.tpl);
				}
			}
		}
		
	}