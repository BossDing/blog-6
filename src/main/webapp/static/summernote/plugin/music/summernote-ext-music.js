(function(factory) {
  /* global define */
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['jquery'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // Node/CommonJS
    module.exports = factory(require('jquery'));
  } else {
    // Browser globals
    factory(window.jQuery);
  }
}(function($) {
  // Extends plugins for adding music.
  //  - plugin is external module for customizing.
  $.extend($.summernote.plugins, {
    /**
     * @param {Object} context - context object has status of editor.
     */
    'music': function(context) {
      var self = this;

      // ui has renders to build ui elements.
      //  - you can create a button with `ui.button`
      var ui = $.summernote.ui;
      
      this.initialize = function() {
    	  
    	  var dialog = '';
    	  dialog += '<div class="modal fade" tabindex="-1" role="dialog">';
    	  dialog += '<div class="modal-dialog" role="document">';
    	  dialog += '<div class="modal-content">';
    	  dialog += '<div class="modal-header">';
    	  dialog += '<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>';
    	  dialog += '<h4 class="modal-title">网易云音乐</h4>';
    	  dialog += '</div>';
    	  dialog += '<div class="modal-body">';
    	  
    	  dialog += '<form>';
    	  dialog += '<div class="form-group">';
    	  dialog += '<label>音乐链接|ID</label>';
    	  dialog += '<input type="text" class="form-control" placeholder="请输入音乐链接或者音乐ID">';
    	  dialog += '</div>';
    	  dialog += '<div class="checkbox">';
    	  dialog += '<label>';
    	  dialog += '<input type="checkbox">自动播放';
    	  dialog += '</label>';
    	  dialog += '</div>';
    	  dialog += '</form>';
    	  
    	  dialog += '</div>';
    	  dialog += '<div class="modal-footer">';
    	  dialog += '<button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>';
    	  dialog += '<button type="button" class="btn btn-primary" data-music>插入</button>';
    	  dialog += '</div>';
    	  dialog += '</div>';
    	  dialog += '</div>';
    	  dialog += '</div>';
    	  
          this.$panel = $(dialog).hide();
          
          var panel = this.$panel;
          
          this.$panel.on('hide.bs.modal',function(){
        	  var form = panel.find('form')[0];
        	  form.reset();
          });
          
          $(panel.find('[data-music]')).click(function(){
        	  var text = panel.find('input[type="text"]').val();
        	  var mid;
        	  if(text.indexOf('http://') > -1 || text.indexOf('https://') > -1){
        		  var idx = text.indexOf('?');
        		  if(idx == -1){
        			  panel.modal('hide');
        		  }else{
        			 try{
        				 var params = text.split('?')[1].split('&');
        				 for(var i=0;i<params.length;i++){
        					 var paramKV = params[i].split('=');
        					 if(paramKV[0] == 'id'){
        						 mid = paramKV[1];
        						 break;
        					 }
        				 }
        			 }catch (e) {
           			  	panel.modal('hide');
					}
        		  }
        	  }else{
        		  mid = text;
        	  }
        	  
        	  if(mid){
        		  var auto = panel.find('input[type="checkbox"]').is(":checked");
        		  var iframe = '<iframe style="display:block" frameborder="no" border="0" marginwidth="0" marginheight="0" height=86 src="//music.163.com/outchain/player?type=2&id='+mid+'&auto='+(auto ? '1' : '0')+'&height=66"></iframe>';
        		  context.invoke('editor.insertNode', $(iframe)[0]);
        	  } 
 			  panel.modal('hide');
        	  
          })

          this.$panel.appendTo('body');
      };

      // add music button
      context.memo('button.music', function() {
        // create button
        var button = ui.button({
          contents: '音乐',
          tooltip: '网易云',
          click: function() {
        	  self.$panel.modal('show');
          }
        });

        // create jQuery object from button instance.
        var $music = button.render();
        return $music;
      });
      
      this.destroy = function() {
          this.$panel.remove();
          this.$panel = null;
        };
    }
  });
}));
