import { useContext, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { ShopContext } from '../context/ShopContext';
import Title from '../components/Title';
import type { UserProfile, UserProfileUpdatePayload } from '../types/shop';

const ProfileField = ({ label, value }: { label: string; value?: string }) => (
    <div className='border-b border-gray-100 py-4'>
        <p className='text-xs uppercase tracking-[0.18em] text-gray-400'>{label}</p>
        <p className='mt-1 text-sm sm:text-base text-gray-800 break-words'>{value || 'Chưa cập nhật'}</p>
    </div>
);

const Profile = () => {
    const context = useContext(ShopContext);
    const token = context?.token;
    const user = context?.user as UserProfile | null;
    const updateUserProfile = context?.updateUserProfile as ((payload: UserProfileUpdatePayload) => Promise<unknown>) | undefined;
    const [isEditing, setIsEditing] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [formData, setFormData] = useState<UserProfileUpdatePayload>({
        id: '',
        firstName: '',
        lastName: '',
        avatar: '',
    });

    if (!token) {
        return <Navigate to='/login' replace />;
    }

    const firstName = user?.f_name || user?.firstName;
    const lastName = user?.l_name || user?.lastName;
    const avatar = user?.img || user?.avatar;
    const displayName = [firstName, lastName].filter(Boolean).join(' ') || 'Khách hàng';
    const previewName = [formData.firstName, formData.lastName].filter(Boolean).join(' ') || displayName;
    const previewAvatar = formData.avatar || avatar;
    const initials = previewName
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map((name) => name[0])
        .join('')
        .toUpperCase();
    const hasProfile = Boolean(user);
    const formChanged = useMemo(() => {
        return (
            formData.firstName !== (firstName || '') ||
            formData.lastName !== (lastName || '') ||
            (formData.avatar || '') !== (avatar || '')
        );
    }, [avatar, firstName, formData, lastName]);

    useEffect(() => {
        setFormData({
            id: user?.id || '',
            firstName: firstName || '',
            lastName: lastName || '',
            avatar: avatar || '',
        });
    }, [avatar, firstName, lastName, user?.id]);

    const handleChange = (field: keyof UserProfileUpdatePayload, value: string) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    const handleCancel = () => {
        setFormData({
            id: user?.id || '',
            firstName: firstName || '',
            lastName: lastName || '',
            avatar: avatar || '',
        });
        setIsEditing(false);
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        const payload = {
            id: user?.id || formData.id,
            firstName: formData.firstName.trim(),
            lastName: formData.lastName.trim(),
            avatar: formData.avatar?.trim(),
        };

        if (!payload.id) {
            toast.error('Không tìm thấy mã người dùng để cập nhật hồ sơ');
            return;
        }

        if (!payload.firstName || !payload.lastName) {
            toast.error('Vui lòng nhập đầy đủ họ và tên');
            return;
        }

        if (!updateUserProfile) {
            toast.error('Chưa cấu hình chức năng cập nhật hồ sơ');
            return;
        }

        setIsSaving(true);
        try {
            await updateUserProfile(payload);
            toast.success('Cập nhật hồ sơ thành công');
            setIsEditing(false);
        } catch (error) {
            console.error(error);
            const message = error instanceof Error ? error.message : 'Không thể cập nhật hồ sơ';
            toast.error(message);
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className='border-t pt-10 sm:pt-14 min-h-[70vh]'>
            <div className='text-2xl mb-8 flex items-center justify-between'>
                <Title text1='HỒ' text2='SƠ' />
            </div>

            <div className='grid lg:grid-cols-[0.9fr_1.4fr] gap-8 items-start'>
                <div className='border bg-white shadow-sm'>
                    <div className='h-28 bg-gradient-to-r from-black via-gray-800 to-gray-500'></div>
                    <div className='px-6 pb-7 -mt-14'>
                        {previewAvatar ? (
                            <img
                                className='w-28 h-28 rounded-full object-cover border-4 border-white bg-white shadow-md'
                                src={previewAvatar}
                                alt={previewName}
                            />
                        ) : (
                            <div className='w-28 h-28 rounded-full border-4 border-white bg-gray-100 flex items-center justify-center text-3xl font-medium text-gray-700 shadow-md'>
                                {initials || 'U'}
                            </div>
                        )}

                        <div className='mt-5'>
                            <p className='text-2xl font-medium text-gray-900'>{previewName}</p>
                            <p className='mt-2 text-sm text-gray-500'>Hồ sơ mua sắm cá nhân</p>
                        </div>

                        <button
                            type='button'
                            onClick={() => setIsEditing(true)}
                            disabled={!hasProfile || isSaving}
                            className='mt-6 w-full border border-black py-3 text-sm hover:bg-black hover:text-white transition-all disabled:cursor-not-allowed disabled:border-gray-200 disabled:text-gray-400 disabled:hover:bg-white'
                        >
                            CHỈNH SỬA HỒ SƠ
                        </button>
                    </div>
                </div>

                <div className='border bg-white px-6 sm:px-8 py-6 shadow-sm'>
                    <div className='flex items-center justify-between border-b pb-5'>
                        <div>
                            <p className='text-lg font-medium text-gray-900'>
                                {isEditing ? 'Chỉnh sửa thông tin' : 'Thông tin tài khoản'}
                            </p>
                            <p className='text-sm text-gray-500 mt-1'>
                                {isEditing ? 'Cập nhật thông tin cá nhân dùng cho tài khoản của bạn' : 'Thông tin khách hàng đã lưu'}
                            </p>
                        </div>
                    </div>

                    {isEditing ? (
                        <form onSubmit={handleSubmit} className='mt-6 space-y-5'>
                            <div className='grid sm:grid-cols-2 gap-5'>
                                <div>
                                    <label className='text-xs uppercase tracking-[0.16em] text-gray-500'>Tên</label>
                                    <input
                                        value={formData.firstName}
                                        onChange={(event) => handleChange('firstName', event.target.value)}
                                        className='mt-2 w-full border border-gray-300 px-4 py-3 outline-none transition-all focus:border-black focus:shadow-[0_0_0_3px_rgba(0,0,0,0.06)]'
                                        placeholder='Nhập tên'
                                    />
                                </div>
                                <div>
                                    <label className='text-xs uppercase tracking-[0.16em] text-gray-500'>Họ</label>
                                    <input
                                        value={formData.lastName}
                                        onChange={(event) => handleChange('lastName', event.target.value)}
                                        className='mt-2 w-full border border-gray-300 px-4 py-3 outline-none transition-all focus:border-black focus:shadow-[0_0_0_3px_rgba(0,0,0,0.06)]'
                                        placeholder='Nhập họ'
                                    />
                                </div>
                            </div>

                            <div>
                                <label className='text-xs uppercase tracking-[0.16em] text-gray-500'>URL ảnh đại diện</label>
                                <input
                                    value={formData.avatar || ''}
                                    onChange={(event) => handleChange('avatar', event.target.value)}
                                    className='mt-2 w-full border border-gray-300 px-4 py-3 outline-none transition-all focus:border-black focus:shadow-[0_0_0_3px_rgba(0,0,0,0.06)]'
                                    placeholder='https://example.com/avatar.jpg'
                                />
                            </div>

                            <p className='text-xs text-gray-500'>
                                Backend hiện chỉ hỗ trợ cập nhật họ, tên và ảnh đại diện. Số điện thoại chưa có trong API cập nhật hồ sơ.
                            </p>

                            <div className='flex flex-col sm:flex-row gap-3 pt-2'>
                                <button
                                    type='submit'
                                    disabled={isSaving || !formChanged}
                                    className='bg-black text-white px-8 py-3 text-sm font-medium transition-all hover:bg-gray-800 disabled:cursor-not-allowed disabled:bg-gray-300'
                                >
                                    {isSaving ? 'ĐANG LƯU...' : 'LƯU THAY ĐỔI'}
                                </button>
                                <button
                                    type='button'
                                    onClick={handleCancel}
                                    disabled={isSaving}
                                    className='border border-gray-300 px-8 py-3 text-sm font-medium transition-all hover:border-black disabled:cursor-not-allowed disabled:text-gray-400'
                                >
                                    HỦY
                                </button>
                            </div>
                        </form>
                    ) : (
                        <div className='mt-2'>
                            <ProfileField label='Mã người dùng' value={user?.id} />
                            <div className='grid sm:grid-cols-2 sm:gap-8'>
                                <ProfileField label='Tên' value={firstName} />
                                <ProfileField label='Họ' value={lastName} />
                            </div>
                            <ProfileField label='Ảnh đại diện' value={avatar} />
                            <ProfileField label='Số điện thoại' value={user?.phone} />
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Profile;
