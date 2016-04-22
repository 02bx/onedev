/*
 * 2016, Robin Shen
 */
(function ( $ ) {
 
    $.fn.clearable = function() {
    	var $input = this;
    	if ($input.next().hasClass("input-clear")) 
    		$input.next().remove();
		$input.addClass("clearable");
		$input.parent().css("position", "relative");
		var left = $input.offset().left - $input.parent().offset().left;
		var top = $input.offset().top - $input.parent().offset().top;
		$input.after("<span class='input-clear'>&nbsp;&nbsp;&nbsp;&nbsp;</span>");
		if ($input.hasClass("input-sm")) {
			$input.next().css({left: left+$input.outerWidth()-20, top: "9px"});
		} else {
			$input.next().css({left: left+$input.outerWidth()-20, top: "11px"});
		}
		$input.next().click(function() {
			$input.val("");
			$(this).hide();
			$input.trigger("inputchange");
		});
		if ($input.val().length != 0)
			$input.next().show();
		
		$input.bind("inputchange", function() {
			var value = $(this).val();
			if (value.trim().length != 0)
				$input.next().show();
			else
				$input.next().hide();
		});
    	return this;
    };
 
}( jQuery ));
