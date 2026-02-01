
import React from "react";
import { createContext, useEffect, useState } from "react";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import { assets } from "../assets/assets";

export const ShopContext = createContext<any>(null);

const ShopContextProvider = (props: { children: React.ReactNode }) => {

    const currency = '$';
    const delivery_fee = 10;
    const [search, setSearch] = useState('');
    const [showSearch, setShowSearch] = useState(false);
    const [cartItems, setCartItems] = useState<any>({});
    const [products, setProducts] = useState<any[]>([
        {
            _id: "aaaaa",
            name: "Premium Cotton T-Shirt",
            description: "A high-quality cotton T-shirt, perfect for everyday wear.",
            price: 100,
            image: [assets.p_img1],
            category: "Men",
            subCategory: "Topwear",
            sizes: ["S", "M", "L"],
            date: 1716634345484,
            bestSeller: true
        },
        {
            _id: "aaaab",
            name: "Athletic Running Shorts",
            description: "Lightweight and breathable shorts for your daily workout.",
            price: 150,
            image: [assets.p_img2_1],
            category: "Men",
            subCategory: "Bottomwear",
            sizes: ["M", "L", "XL"],
            date: 1716621345484,
            bestSeller: true
        },
        {
            _id: "aaaac",
            name: "Classic Denim Jacket",
            description: "A timeless denim jacket that goes with any outfit.",
            price: 210,
            image: [assets.p_img3],
            category: "Unisex",
            subCategory: "Topwear",
            sizes: ["S", "M", "L", "XL"],
            date: 1716622345484,
            bestSeller: true
        },
        {
            _id: "aaaad",
            name: "Performance Sports Hoodie",
            description: "Engineered for style and performance.",
            price: 180,
            image: [assets.p_img4],
            category: "Men",
            subCategory: "Topwear",
            sizes: ["L", "XL", "XXL"],
            date: 1716623345484,
            bestSeller: true
        },
        {
            _id: "aaaae",
            name: "Summer Floral Dress",
            description: "A beautiful floral dress perfect for summer days.",
            price: 250,
            image: [assets.p_img1],
            category: "Women",
            subCategory: "Dress",
            sizes: ["S", "M", "L"],
            date: 1716624345484,
            bestSeller: true
        },
        {
            _id: "aaaaf",
            name: "Casual Slim Fit Jeans",
            description: "Stylish slim fit jeans for a modern look.",
            price: 160,
            image: [assets.p_img2_1],
            category: "Men",
            subCategory: "Bottomwear",
            sizes: ["30", "32", "34"],
            date: 1716625345484,
            bestSeller: true
        },
        {
            _id: "aaaag",
            name: "Running Shoes",
            description: "Comfortable running shoes for your daily jog.",
            price: 120,
            image: [assets.p_img3],
            category: "Unisex",
            subCategory: "Footwear",
            sizes: ["8", "9", "10"],
            date: 1716626345484,
            bestSeller: true
        },
        {
            _id: "aaaah",
            name: "Leather Wallet",
            description: "A premium leather wallet to keep your cards safe.",
            price: 50,
            image: [assets.p_img4],
            category: "Accessories",
            subCategory: "Wallet",
            sizes: [],
            date: 1716627345484,
            bestSeller: false
        }
    ]);
    const navigate = useNavigate();

    const addToCart = async (itemId: string, size: string) => {

        if (!size) {
            toast.error('Vui lòng chọn size');
            return;
        }

        let cartData = structuredClone(cartItems);

        if (cartData[itemId]) {
            if (cartData[itemId][size]) {
                cartData[itemId][size] += 1;
            }
            else {
                cartData[itemId][size] = 1;
            }
        }
        else {
            cartData[itemId] = {};
            cartData[itemId][size] = 1;
        }
        setCartItems(cartData);
        toast.success('Thêm vào giỏ hàng thành công');
    }

    const getCartCount = () => {
        let totalCount = 0;
        for (const items in cartItems) {
            for (const item in cartItems[items]) {
                try {
                    if (cartItems[items][item] > 0) {
                        totalCount += cartItems[items][item];
                    }
                } catch (error) {

                }
            }
        }
        return totalCount;
    }

    const updateQuantity = async (itemId: string, size: string, quantity: number) => {
        let cartData = structuredClone(cartItems);
        cartData[itemId][size] = quantity;
        setCartItems(cartData);
    }

    const getCartAmount = () => {
        let totalAmount = 0;
        for (const items in cartItems) {
            let itemInfo = products.find((product) => product._id === items);
            for (const item in cartItems[items]) {
                try {
                    if (cartItems[items][item] > 0 && itemInfo) {
                        totalAmount += itemInfo.price * cartItems[items][item];
                    }
                } catch (error) {

                }
            }
        }
        return totalAmount;
    }

    const value = {
        products, currency, delivery_fee,
        search, setSearch, showSearch, setShowSearch,
        cartItems, addToCart,
        getCartCount, updateQuantity, getCartAmount,
        navigate
    }

    return (
        <ShopContext.Provider value={value}>
            {props.children}
        </ShopContext.Provider>
    )

}

export default ShopContextProvider;
