import { useContext, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ShopContext } from '../context/ShopContext';
import { assets } from '../assets/assets';
import type { Product as ProductType } from '../types/shop';
import RelatedProducts from '../components/RelatedProducts'; // RelatedProducts is already converted
import { formatCurrency } from '../lib/format';

const Product = () => {
    const params = useParams(); // useParams returns a string based on route definitions
    const productId = params.productId;

    const context = useContext(ShopContext);
    const products = (context?.products || []) as ProductType[];
    const addToCart = context?.addToCart; // Safe access

    const [productData, setProductData] = useState<ProductType | null>(null);
    const [image, setImage] = useState('');
    const [size, setSize] = useState('');

    const fetchProductData = () => {
        // Note: In assets.ts we defined `_id`. Ensure usage matches.
        const product = products.find((item) => item._id === productId);
        if (product) {
            setProductData(product);
            setImage(product.image[0]); // assets.ts uses 'image' not 'images'
        }
    };

    useEffect(() => {
        fetchProductData();
    }, [productId, products]);

    return productData ? (
        <div className="border-t-2 pt-10 transition-opacity ease-in duration-500 opacity-100">
            {/* Product Section */}
            <div className="flex flex-col sm:flex-row gap-12">
                {/* Left Section: Images */}
                <div className="flex-1 flex flex-col sm:flex-row gap-3">
                    {/* Thumbnails */}
                    <div className="flex sm:flex-col overflow-x-auto sm:overflow-y-auto sm:w-[20%] w-full gap-2">
                        {productData.image.map((item, index) => (
                            <img
                                onClick={() => setImage(item)}
                                src={item}
                                key={index}
                                className={`w-24 h-24 object-cover cursor-pointer border ${image === item ? 'border-orange-500' : 'border-gray-200'
                                    }`}
                                alt={`Thumbnail ${index + 1}`}
                            />
                        ))}
                    </div>
                    {/* Main Image */}
                    <div className="w-full sm:w-[80%]">
                        <img src={image} className="w-full h-auto border border-gray-200" alt={productData.name} />
                    </div>
                </div>

                {/* Right Section: Product Details */}
                <div className="flex-1">
                    <h1 className="font-medium text-2xl mt-2">{productData.name}</h1>
                    <div className="flex items-center gap-1 mt-2">
                        <img src={assets.star_icon} className="w-3.5" alt="Star" />
                        <img src={assets.star_icon} className="w-3.5" alt="Star" />
                        <img src={assets.star_icon} className="w-3.5" alt="Star" />
                        <img src={assets.star_icon} className="w-3.5" alt="Star" />
                        <img src={assets.star_dull_icon} className="w-3.5" alt="Dull Star" />
                        <p className="pl-2">122</p>
                    </div>
                    <p className="mt-5 text-3xl font-medium">
                        {formatCurrency(productData.price)}
                    </p>
                    <p className="mt-5 text-gray-500">{productData.description}</p>
                    <div className="flex flex-col gap-4 my-8">
                        <p>Chọn size</p>
                        <div className="flex gap-2">
                            {productData.sizes.length > 0
                                ? productData.sizes.map((item, index) => (
                                    <button
                                        onClick={() => setSize(item)}
                                        key={index}
                                        className={`bg-gray-100 py-2 px-4 border ${item === size ? 'border-orange-500' : ''
                                            }`}
                                    >
                                        {item}
                                    </button>
                                ))
                                : <p className="text-sm text-gray-500">Hết hàng</p>
                            }
                        </div>
                    </div>
                    <button
                        onClick={() => addToCart && addToCart(productData._id, size)} // Safe call
                        disabled={productData.sizes.length === 0}
                        className="bg-black text-white px-8 py-3 text-sm active:bg-gray-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                    >
                        {productData.sizes.length > 0 ? 'THÊM VÀO GIỎ' : 'TẠM HẾT HÀNG'}
                    </button>
                    <hr className="mt-8 sm:w-4/5" />
                    <div className="text-sm text-gray-500 mt-5 flex flex-col gap-1">
                        <p>Sản phẩm chính hãng 100%.</p>
                        <p>Hỗ trợ thanh toán khi nhận hàng.</p>
                        <p>Dễ dàng đổi trả trong vòng 7 ngày.</p>
                    </div>
                </div>
            </div>

            {/* Description and Reviews */}
            <div className="mt-20">
                <div className="flex">
                    <b className="border px-5 py-3 text-sm">Mô tả</b>
                    <p className="border px-5 py-3 text-sm">Đánh giá (122)</p>
                </div>
                <div className="flex flex-col gap-4 border px-6 py-6 text-sm text-gray-500">
                    <p>
                        Sản phẩm được chọn lọc nhằm mang lại trải nghiệm mua sắm tiện lợi,
                        dễ phối đồ và phù hợp với nhu cầu sử dụng hằng ngày.
                    </p>
                    <p>
                        Vui lòng chọn đúng size trước khi thêm vào giỏ hàng để hệ thống kiểm tra tồn kho chính xác.
                    </p>
                </div>
            </div>

            {/* Related Products Section */}
            <RelatedProducts
                category={productData.category}
                subCategory={productData.subCategory}
            />
        </div>
    ) : (
        <div className="opacity-0"></div>
    );
};

export default Product;
