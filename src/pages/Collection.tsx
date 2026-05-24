import { useContext, useMemo } from 'react';
import { ShopContext } from '../context/ShopContext';
import ProductItem from '../components/ProductItem';
import Title from '../components/Title';
import type { Product } from '../types/shop';

const Collection = () => {
    const context = useContext(ShopContext);
    const products = (context?.products || []) as Product[];
    const search = (context?.search || '').trim().toLowerCase();

    const filteredProducts = useMemo(() => {
        if (!search) return products;

        return products.filter((product) =>
            product.name.toLowerCase().includes(search) ||
            product.description.toLowerCase().includes(search) ||
            product.category.toLowerCase().includes(search)
        );
    }, [products, search]);

    return (
        <div className='border-t pt-10'>
            <div className='flex justify-between text-base sm:text-2xl mb-4'>
                <Title text1='TẤT CẢ' text2='SẢN PHẨM' />
            </div>

            {filteredProducts.length === 0 ? (
                <p className='py-16 text-center text-gray-500'>Không tìm thấy sản phẩm.</p>
            ) : (
                <div className='grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 gap-y-6'>
                    {filteredProducts.map((item) => (
                        <ProductItem
                            key={item._id}
                            id={item._id}
                            image={item.image}
                            name={item.name}
                            price={item.price}
                            sizes={item.sizes}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

export default Collection;
