import Data.List (sort)
import System.Environment (getArgs)

main :: IO ()
main = do
    args <- getArgs
    let nums = sort (map read args :: [Int])
    putStrLn (unwords (map show nums))
