import request from './user'

export const searchNovel = (keyword) => {
  return request.get('/assist/novel/search', { params: { keyword } })
}

export const getRank = () => {
  return request.get('/assist/rank')
}

export const analyzeWork = (workName) => {
  return request.get('/assist/analyze', { params: { workName } })
}