document.addEventListener('DOMContentLoaded', () => {
    // Hộp thoại xác nhận cho các thao tác quan trọng (xóa, khóa, trả toàn bộ)
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    // Select chuyển trang (ví dụ chọn số bản ghi mỗi trang)
    document.querySelectorAll('select[data-navigate]').forEach(select => {
        select.addEventListener('change', () => {
            window.location.href = select.value;
        });
    });

    // Tự submit bộ lọc khi đổi select
    document.querySelectorAll('[data-auto-submit]').forEach(input => {
        input.addEventListener('change', () => input.form.submit());
    });

    const toggle = document.querySelector('[data-toggle-sidebar]');
    if (toggle) {
        toggle.addEventListener('click', () => document.getElementById('sidebar').classList.toggle('show'));
    }

    // Tự ẩn thông báo thành công sau vài giây
    document.querySelectorAll('.alert-success.alert-dismissible').forEach(alert => {
        setTimeout(() => bootstrap.Alert.getOrCreateInstance(alert).close(), 6000);
    });
});
