var createEditor = function(editorId){
		var mixedMode = {
			name: "htmlmixed",
			scriptTypes: [{matches: /\/x-handlebars-template|\/x-mustache/i,
					   mode: null},
					  {matches: /(text|application)\/(x-)?vb(a|script)/i,
					   mode: "vbscript"}]
		};
		var editor = CodeMirror.fromTextArea(document.getElementById(editorId), {
			mode: mixedMode,
			lineNumbers: true,
			autoCloseTags: true,
			allowDropFileTypes:['text/html'],
			extraKeys: {"Alt-/": "autocomplete","Alt-F": "findPersistent","Ctrl-A":"selectAll"}
		});
		
		var wrap = function(url){
			var ext = url.split('.').pop().toLowerCase();
			if(ext == 'css'){
				return '<link href="'+url+'" rel="stylesheet">';
			}
			if(isImage(ext)){
				return '<img src="'+url+'" />';
			}
			if(ext == 'js'){
				return '<script src="'+url+'"></script>';
			}
			return '<a href="'+url+'">'+url+'</a>';
		}
		
		var isImage = function(ext){
			return ext == 'png' || ext == 'jpg' || ext == 'jpeg' || ext == 'gif';
		}
		return {
			setSize:function(width,height){
				editor.setSize(width,height);
			},
			format : function(){
				var selection = editor.getSelection();
				if($.trim(selection) != ''){
					var formatted = html_beautify(selection);
					editor.replaceSelection(formatted);
				} else {
					editor.setValue(html_beautify(editor.getValue()));
				}
			},
			getValue : function(){
				return editor.getValue();
			},
			insertUrl : function(url,smart){
				if(smart){
					editor.replaceSelection(wrap(url));
				}else{
					editor.replaceSelection(url);
				}
			},
			clear:function(){
				editor.setValue('');
			},
			replaceSelection : function(str){
				editor.replaceSelection(str);
			},
			setValue : function(str){
				editor.setValue(str);
			}
		}
	};