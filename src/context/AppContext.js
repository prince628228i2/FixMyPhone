import React,{createContext,useContext,useMemo,useState} from 'react';
const C=createContext(null);
export function AppProvider({children}){const [mode,setMode]=useState('home'); return <C.Provider value={useMemo(()=>({mode,setMode}),[mode])}>{children}</C.Provider>}
export const useAppContext=()=>useContext(C);
