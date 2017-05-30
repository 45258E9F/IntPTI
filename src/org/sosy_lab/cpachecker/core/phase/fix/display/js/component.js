// initialization
$(document).ready(function() {
  $('.ui.dropdown').dropdown({
    on: 'click'
  });
  $('.circular.ui.button').on('click', function() {
    if ($(this).hasClass('active') && $('#mode_selected').text() === 'Manual') {
      $(this).removeClass('active');
    } else {
      $(this).addClass('active');
    }
  });
});