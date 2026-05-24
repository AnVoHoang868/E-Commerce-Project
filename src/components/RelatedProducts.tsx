import { useContext, useEffect, useState } from 'react';
import { ShopContext } from '../context/ShopContext';
import ProductItem from './ProductItem';
import Title from './Title';
import type { Product } from '../types/shop';

interface RelatedProductsProps {
    category: string;
    subCategory: string;
}

const RelatedProducts = ({ category, subCategory }: RelatedProductsProps) => {

    const context = useContext(ShopContext);
    const [related, setRelated] = useState<Product[]>([]);

    useEffect(() => {
        if (context && context.products.length > 0) {
            let productsCopy = (context.products as Product[]).slice();
            productsCopy = productsCopy.filter((item) => category === item.category);
            productsCopy = productsCopy.filter((item) => subCategory === item.subCategory);
            setRelated(productsCopy.slice(0, 5));
        }
    }, [context, category, subCategory]); // context added to dependency array

    return (
        <div className="my-24">
            <div className="text-center text-3xl py-2">
                <Title text1={'SẢN PHẨM'} text2={'LIÊN QUAN'} />
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-y-6">
                {related.map((item, index) => (
                    <ProductItem
                        key={index}
                        id={item._id}
                        name={item.name}
                        price={item.price}
                        image={item.image}
                        sizes={item.sizes}
                    />
                ))}
            </div>
        </div>
    );
};

export default RelatedProducts;
